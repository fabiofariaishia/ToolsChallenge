package br.com.sicredi.toolschallenge.estorno.service;

import br.com.sicredi.toolschallenge.estorno.domain.Estorno;
import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoRequestDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoResponseDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoMapper;
import br.com.sicredi.toolschallenge.estorno.events.EstornoCriadoEvento;
import br.com.sicredi.toolschallenge.estorno.events.EstornoStatusAlteradoEvento;
import br.com.sicredi.toolschallenge.estorno.repository.EstornoRepository;
import br.com.sicredi.toolschallenge.infra.outbox.publisher.EventoPublisher;
import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.repository.PagamentoRepository;
import br.com.sicredi.toolschallenge.shared.exception.NegocioException;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import br.com.sicredi.toolschallenge.adquirente.service.AdquirenteService;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.shared.config.ReprocessamentoProperties;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service para operações de negócio de Estorno.
 * 
 * Responsabilidades:
 * - Criar solicitação de estorno com lock distribuído (Redisson)
 * - Validar regras de negócio (pagamento autorizado, prazo 24h, valor correto)
 * - Processar estorno via AdquirenteService (Circuit Breaker + Retry + Bulkhead)
 * - Consultar estornos
 * 
 * Lock Distribuído:
 * - Previne race conditions em estornos concorrentes
 * - Chave: "lock:estorno:{idTransacao}"
 * - Timeout: 5 segundos (waitTime)
 * - Lease: 30 segundos (renovado automaticamente pelo watchdog)
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EstornoService {

    private final EstornoRepository repository;
    private final PagamentoRepository pagamentoRepository;
    private final EstornoMapper mapper;
    private final EventoPublisher eventoPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final AdquirenteService adquirenteService;
    private final ReprocessamentoProperties reprocessamentoProperties;
    private final MeterRegistry meterRegistry;
    private final Random random = new Random();
    
    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * Cria uma nova solicitação de estorno com lock distribuído.
     * 
     * Lock Distribuído (Redisson):
     * - Chave: "lock:estorno:{idTransacao}"
     * - WaitTime: 5 segundos (quanto tempo espera para adquirir)
     * - LeaseTime: 30 segundos (quanto tempo mantém, renovado por watchdog)
     * - Previne race conditions em requisições concorrentes
     * 
     * Validações (executadas DENTRO do lock):
     * 1. Pagamento existe
     * 2. Pagamento está AUTORIZADO
     * 3. Valor do estorno = valor do pagamento (estorno parcial não permitido)
     * 4. Dentro da janela de 24 horas
     * 5. Não existe estorno CANCELADO para este pagamento
     * 
     * @param request DTO com dados do estorno
     * @return DTO de resposta com status do estorno
     * @throws NegocioException se validações falharem ou timeout ao adquirir lock
     */
    @Transactional
    @Timed(value = "estorno.processar.latency", description = "Latência para processar estorno")
    @Counted(value = "estorno.processados.total", description = "Total de estornos processados")
    public EstornoResponseDTO criarEstorno(EstornoRequestDTO request) {
        String idTransacao = request.getIdTransacao();
        log.info("Criando estorno para transação: {}", idTransacao);
        
        // Criar lock distribuído para este pagamento (se disponível)
        RLock lock = redissonClient != null ? redissonClient.getLock("lock:estorno:" + idTransacao) : null;
        
        try {
            // Tentar adquirir lock (se disponível)
            if (lock != null) {
                boolean adquirido = lock.tryLock(5, 30, TimeUnit.SECONDS);
                
                if (!adquirido) {
                    log.warn("Timeout ao adquirir lock para estorno: {}", idTransacao);
                    throw new NegocioException(
                        "Sistema ocupado processando este pagamento. Tente novamente em instantes."
                    );
                }
                
                log.debug("Lock adquirido para estorno: {}", idTransacao);
            } else {
                log.warn("Lock distribuído NÃO disponível - Race conditions possíveis!");
            }
            
            // TODAS as validações DENTRO do lock para prevenir race conditions
            
            // 1. Buscar pagamento original
            Pagamento pagamento = pagamentoRepository.findByIdTransacao(idTransacao)
                .orElseThrow(() -> {
                    log.warn("Pagamento não encontrado: {}", idTransacao);
                    return new RecursoNaoEncontradoException("Pagamento", idTransacao);
                });

            // 2. Validar se pagamento está AUTORIZADO
            if (pagamento.getStatus() != StatusPagamento.AUTORIZADO) {
                log.warn("Tentativa de estornar pagamento com status {}: {}", 
                    pagamento.getStatus(), idTransacao);
                throw new NegocioException(
                    "Apenas pagamentos AUTORIZADOS podem ser estornados. Status atual: " + pagamento.getStatus()
                );
            }

            // 3. Validar valor (deve ser estorno total)
            if (request.getValor().compareTo(pagamento.getValor()) != 0) {
                log.warn("Valor de estorno (R$ {}) diferente do valor do pagamento (R$ {})", 
                    request.getValor(), pagamento.getValor());
                throw new NegocioException(
                    String.format("Estorno parcial não permitido. Valor do pagamento: R$ %.2f", 
                        pagamento.getValor())
                );
            }

            // 4. Validar janela de 24 horas
            if (pagamento.getDataHora() == null) {
                log.error("Pagamento {} sem data/hora válida", idTransacao);
                throw new NegocioException("Dados de pagamento inválidos - sem data de criação");
            }
            
            OffsetDateTime agora = OffsetDateTime.now();
            Duration tempoDecorrido = Duration.between(pagamento.getDataHora(), agora);
            if (tempoDecorrido.toHours() > 24) {
                log.warn("Estorno solicitado após 24h. Pagamento: {}, Horas decorridas: {}", 
                    idTransacao, tempoDecorrido.toHours());
                throw new NegocioException(
                    "Estorno só pode ser solicitado dentro de 24 horas. Tempo decorrido: " + 
                    tempoDecorrido.toHours() + " horas"
                );
            }

            // 5. Verificar se já existe estorno CANCELADO para este pagamento
            boolean existeEstornoCancelado = repository.existsEstornoCanceladoByIdTransacaoPagamento(idTransacao);
            if (existeEstornoCancelado) {
                log.warn("Já existe estorno CANCELADO para o pagamento: {}", idTransacao);
                throw new NegocioException(
                    "Já existe um estorno processado para este pagamento"
                );
            }

            // 6. Criar estorno
            Estorno estorno = mapper.paraEntidade(request);
            estorno.setIdEstorno(UUID.randomUUID().toString());
            estorno.setDataHora(agora);
            estorno.setStatus(StatusEstorno.PENDENTE);
            estorno.setSnowflakeId(gerarSnowflakeId());

            // Salvar como PENDENTE
            estorno = repository.save(estorno);
            log.info("Estorno criado com ID: {}, Status: PENDENTE", estorno.getIdEstorno());

            // Publicar evento: Estorno Criado
            publicarEventoEstornoCriado(estorno, pagamento);

            // 7. Processar estorno com adquirente (Circuit Breaker + Retry + Bulkhead)
            StatusEstorno statusAnterior = estorno.getStatus();
            processarEstornoComAdquirente(estorno, pagamento);

            // Salvar com novo status
            estorno = repository.save(estorno);

            // Publicar evento: Status Alterado (se mudou)
            if (!statusAnterior.equals(estorno.getStatus())) {
                publicarEventoStatusAlterado(estorno, pagamento, statusAnterior);
            }

            log.info("Estorno {} finalizado - Status: {}", 
                estorno.getIdEstorno(), estorno.getStatus());

            return mapper.paraDTO(estorno, pagamento);
            
        } catch (InterruptedException e) {
            // Restaurar flag de interrupção da thread
            Thread.currentThread().interrupt();
            log.error("Thread interrompida ao processar estorno: {}", idTransacao, e);
            throw new NegocioException(
                "Processamento do estorno foi interrompido. Tente novamente."
            );
        } finally {
            // Liberar lock SEMPRE (se esta thread o possui e lock está disponível)
            if (lock != null && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock liberado para estorno: {}", idTransacao);
            }
        }
    }

    /**
     * Busca estorno por ID.
     * 
     * @param idEstorno UUID do estorno
     * @return DTO de resposta
     */
    public EstornoResponseDTO buscarPorIdEstorno(String idEstorno) {
        log.info("Buscando estorno por ID: {}", idEstorno);

        Estorno estorno = repository.findByIdEstorno(idEstorno)
            .orElseThrow(() -> {
                log.warn("Estorno não encontrado: {}", idEstorno);
                return new RecursoNaoEncontradoException("Estorno", idEstorno);
            });

        // Buscar pagamento original para montar TransacaoDTO completa
        Pagamento pagamento = pagamentoRepository.findByIdTransacao(estorno.getIdTransacao())
            .orElseThrow(() -> {
                log.error("Pagamento não encontrado para estorno: {}", estorno.getIdTransacao());
                return new RecursoNaoEncontradoException("Pagamento", estorno.getIdTransacao());
            });

        return mapper.paraDTO(estorno, pagamento);
    }

    /**
     * Lista estornos de um pagamento específico.
     * 
     * @param idTransacao UUID da transação do pagamento
     * @return Lista de DTOs de resposta
     */
    public List<EstornoResponseDTO> listarPorIdTransacao(String idTransacao) {
        log.info("Listando estornos do pagamento: {}", idTransacao);

        // Buscar pagamento uma única vez
        Pagamento pagamento = pagamentoRepository.findByIdTransacao(idTransacao)
            .orElseThrow(() -> {
                log.warn("Pagamento não encontrado: {}", idTransacao);
                return new RecursoNaoEncontradoException("Pagamento", idTransacao);
            });

        List<Estorno> estornos = repository.findByIdTransacaoPagamento(idTransacao);

        return estornos.stream()
            .map(estorno -> mapper.paraDTO(estorno, pagamento))
            .collect(Collectors.toList());
    }

    /**
     * Lista todos os estornos (últimos 100).
     * 
     * @return Lista de DTOs de resposta
     */
    public List<EstornoResponseDTO> listarEstornos() {
        log.info("Listando últimos estornos");

        List<Estorno> estornos = repository.findUltimosEstornos();

        return estornos.stream()
            .map(estorno -> {
                // Buscar pagamento para cada estorno
                Pagamento pagamento = pagamentoRepository.findByIdTransacao(estorno.getIdTransacao())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", estorno.getIdTransacao()));
                return mapper.paraDTO(estorno, pagamento);
            })
            .collect(Collectors.toList());
    }

    /**
     * Lista estornos por status.
     * 
     * @param status Status do estorno
     * @return Lista de DTOs de resposta
     */
    public List<EstornoResponseDTO> listarPorStatus(StatusEstorno status) {
        log.info("Listando estornos por status: {}", status);

        List<Estorno> estornos = repository.findByStatusOrderByCriadoEmDesc(status);

        return estornos.stream()
            .map(estorno -> {
                // Buscar pagamento para cada estorno
                Pagamento pagamento = pagamentoRepository.findByIdTransacao(estorno.getIdTransacao())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", estorno.getIdTransacao()));
                return mapper.paraDTO(estorno, pagamento);
            })
            .collect(Collectors.toList());
    }

    /**
     * Processa estorno com adquirente via AdquirenteService.
     * 
     * Aplica padrões de resiliência:
     * - Circuit Breaker: Protege contra falhas em cascata
     * - Retry: Tenta até 3x em caso de falha transitória
     * - Bulkhead: Isola pool de threads
     * 
     * Mapeamento de Status:
     * - StatusAutorizacao.AUTORIZADO → StatusEstorno.CANCELADO (estorno aprovado) + NSU + Código
     * - StatusAutorizacao.NEGADO → StatusEstorno.NEGADO (estorno recusado)
     * - StatusAutorizacao.PENDENTE → StatusEstorno.PENDENTE (Circuit Breaker OPEN ou timeout)
     * 
     * Estornos PENDENTE devem ser reprocessados posteriormente via scheduler.
     * 
     * @param estorno Estorno a ser processado
     * @param pagamento Pagamento original a ser estornado
     */
    private void processarEstornoComAdquirente(Estorno estorno, Pagamento pagamento) {
        log.info("Processando estorno {} com adquirente", estorno.getIdEstorno());
        
        try {
            // Criar request para adquirente
            AutorizacaoRequest request = new AutorizacaoRequest(
                pagamento.getCartaoMascarado(),
                "***", // CVV não armazenado (PCI-DSS compliance)
                "12/2030", // Validade simulada
                estorno.getValor(),
                pagamento.getEstabelecimento()
            );
            
            // Chamar adquirente (Circuit Breaker + Retry + Bulkhead)
            AutorizacaoResponse response = adquirenteService.processarEstorno(request);
            
            // Mapear StatusAutorizacao → StatusEstorno
            if (response.status() == StatusAutorizacao.AUTORIZADO) {
                estorno.setStatus(StatusEstorno.CANCELADO); // AUTORIZADO = estorno aprovado
                estorno.setNsu(response.nsu());
                estorno.setCodigoAutorizacao(response.codigoAutorizacao());
                log.info("Estorno CANCELADO (aprovado) - ID: {}, NSU: {}, Código: {}", 
                    estorno.getIdEstorno(), response.nsu(), response.codigoAutorizacao());
                
            } else if (response.status() == StatusAutorizacao.NEGADO) {
                estorno.setStatus(StatusEstorno.NEGADO);
                log.warn("Estorno NEGADO - ID: {}", estorno.getIdEstorno());
                
            } else if (response.status() == StatusAutorizacao.PENDENTE) {
                estorno.setStatus(StatusEstorno.PENDENTE);
                log.warn("Estorno PENDENTE (Circuit Breaker ou timeout) - ID: {}", 
                    estorno.getIdEstorno());
            }
            
        } catch (Exception ex) {
            // Fallback: Manter PENDENTE se erro inesperado
            estorno.setStatus(StatusEstorno.PENDENTE);
            log.error("Erro ao processar estorno {} - Mantendo PENDENTE: {}", 
                estorno.getIdEstorno(), ex.getMessage());
        }
    }

    /**
     * Gera Snowflake ID simulado (time-sortable unique ID).
     * 
     * @return Snowflake ID (15 dígitos)
     */
    private Long gerarSnowflakeId() {
        long timestamp = System.currentTimeMillis() % 100000000L;
        long randomPart = random.nextInt(10000000);
        return (timestamp * 10000000L) + randomPart;
    }

    /**
     * Publica evento de estorno criado no outbox.
     * 
     * @param estorno Estorno criado
     * @param pagamento Pagamento relacionado
     */
    private void publicarEventoEstornoCriado(Estorno estorno, Pagamento pagamento) {
        try {
            EstornoCriadoEvento evento = EstornoCriadoEvento.builder()
                    .idEstorno(estorno.getId())
                    .idPagamento(pagamento.getId())
                    .idTransacao(estorno.getIdEstorno())
                    .valorEstorno(estorno.getValor())
                    .motivo(estorno.getMotivo())
                    .status(estorno.getStatus().name())
                    .criadoEm(estorno.getDataHora())
                    .build();

            // Publicar para Outbox (Kafka)
            eventoPublisher.publicarEstornoCriado(evento);
            
            // Publicar para Auditoria (Event Listener)
            eventPublisher.publishEvent(evento);
            
        } catch (Exception e) {
            log.error("Erro ao publicar evento de estorno criado: {}", estorno.getIdEstorno(), e);
            // Não propaga exception para não impactar fluxo principal
        }
    }

    /**
     * Publica evento de status alterado no outbox.
     * 
     * @param estorno Estorno atualizado
     * @param pagamento Pagamento relacionado
     * @param statusAnterior Status anterior
     */
    private void publicarEventoStatusAlterado(Estorno estorno, Pagamento pagamento, StatusEstorno statusAnterior) {
        try {
            EstornoStatusAlteradoEvento evento = EstornoStatusAlteradoEvento.builder()
                    .idEstorno(estorno.getId())
                    .idPagamento(pagamento.getId())
                    .idTransacao(estorno.getIdEstorno())
                    .statusAnterior(statusAnterior.name())
                    .statusNovo(estorno.getStatus().name())
                    .alteradoEm(OffsetDateTime.now())
                    .motivo("Processamento de estorno")
                    .build();

            // Publicar para Outbox (Kafka)
            eventoPublisher.publicarEstornoStatusAlterado(evento);
            
            // Publicar para Auditoria (Event Listener)
            eventPublisher.publishEvent(evento);
            
        } catch (Exception e) {
            log.error("Erro ao publicar evento de status alterado: {}", estorno.getIdEstorno(), e);
            // Não propaga exception para não impactar fluxo principal
        }
    }

    /**
     * Reprocessa estornos pendentes.
     * 
     * <p>Busca todos os estornos com status PENDENTE (que falharam anteriormente devido a
     * Circuit Breaker aberto, timeout ou erro temporário) e tenta reprocessá-los com o adquirente.
     * 
     * <p>Comportamento:
     * <ul>
     *   <li>Verifica se não atingiu maxTentativas (Dead Letter Queue)</li>
     *   <li>Incrementa contador tentativasReprocessamento</li>
     *   <li>Busca estornos PENDENTE ordenados por data de criação (FIFO)</li>
     *   <li>Para cada estorno, tenta processar com adquirente</li>
     *   <li>Atualiza status baseado na resposta: CANCELADO, NEGADO ou mantém PENDENTE</li>
     *   <li>Publica eventos de status alterado</li>
     *   <li>Log de métricas (total, sucessos, falhas, DLQ)</li>
     * </ul>
     * 
     * <p>Transações que atingem maxTentativas param de ser reprocessadas (DLQ).
     * 
     * <p>Chamado automaticamente pelo ReprocessamentoScheduler a cada intervalo configurado.
     * 
     * @see br.com.sicredi.toolschallenge.infra.scheduled.ReprocessamentoScheduler
     * @see ReprocessamentoProperties#getMaxTentativas()
     */
    @Transactional
    public void reprocessarEstornosPendentes() {
        log.info("Iniciando reprocessamento de estornos pendentes");
        
        int maxTentativas = reprocessamentoProperties.getMaxTentativas();
        
        List<Estorno> estornosPendentes = repository.findEstornosPendentes();
        
        if (estornosPendentes.isEmpty()) {
            log.info("Nenhum estorno pendente para reprocessar");
            return;
        }
        
        log.info("Encontrados {} estorno(s) pendente(s) para reprocessar", estornosPendentes.size());
        
        int sucessos = 0;
        int falhas = 0;
        int mantidosPendentes = 0;
        int enviadosDLQ = 0;
        
        for (Estorno estorno : estornosPendentes) {
            try {
                // Verificar se atingiu limite de tentativas (DLQ)
                if (estorno.getTentativasReprocessamento() >= maxTentativas) {
                    enviadosDLQ++;
                    
                    // Incrementar métrica de DLQ
                    meterRegistry.counter("reprocessamento.dlq.total", 
                        "tipo", "estorno").increment();
                    
                    log.warn("Estorno {} atingiu máximo de tentativas ({}) - ENVIADO PARA DLQ - Requer análise manual",
                        estorno.getIdEstorno(), maxTentativas);
                    continue;
                }
                
                log.debug("Reprocessando estorno: {} (tentativa {}/{})", 
                    estorno.getIdEstorno(), 
                    estorno.getTentativasReprocessamento() + 1, 
                    maxTentativas);
                
                StatusEstorno statusAnterior = estorno.getStatus();
                
                // Incrementar contador de tentativas
                estorno.setTentativasReprocessamento(estorno.getTentativasReprocessamento() + 1);
                
                // Buscar pagamento original
                Pagamento pagamento = pagamentoRepository.findByIdTransacao(estorno.getIdTransacao())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", estorno.getIdTransacao()));
                
                // Tentar reprocessar com adquirente
                processarEstornoComAdquirente(estorno, pagamento);
                
                // Salvar novo status
                repository.save(estorno);
                
                // Publicar evento se status mudou
                if (!statusAnterior.equals(estorno.getStatus())) {
                    publicarEventoStatusAlterado(estorno, pagamento, statusAnterior);
                }
                
                // Contabilizar resultado
                if (estorno.getStatus() == StatusEstorno.CANCELADO) {
                    sucessos++;
                    log.info("Estorno reprocessado com SUCESSO (CANCELADO após {} tentativa(s)): {}", 
                        estorno.getTentativasReprocessamento(), estorno.getIdEstorno());
                } else if (estorno.getStatus() == StatusEstorno.NEGADO) {
                    falhas++;
                    log.warn("Estorno reprocessado com FALHA (NEGADO após {} tentativa(s)): {}", 
                        estorno.getTentativasReprocessamento(), estorno.getIdEstorno());
                } else if (estorno.getStatus() == StatusEstorno.PENDENTE) {
                    mantidosPendentes++;
                    log.warn("Estorno mantido PENDENTE após reprocessamento (tentativa {}/{}): {}", 
                        estorno.getTentativasReprocessamento(), maxTentativas, estorno.getIdEstorno());
                }
                
            } catch (Exception e) {
                mantidosPendentes++;
                log.error("Erro ao reprocessar estorno {}: {}", estorno.getIdEstorno(), e.getMessage(), e);
            }
        }
        
        log.info("Reprocessamento de estornos concluído - Total: {}, Sucessos: {}, Falhas: {}, Ainda Pendentes: {}, Enviados para DLQ: {}",
            estornosPendentes.size(), sucessos, falhas, mantidosPendentes, enviadosDLQ);
    }
}
