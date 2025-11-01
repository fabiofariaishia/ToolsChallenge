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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para operações de negócio de Estorno.
 * 
 * Responsabilidades:
 * - Criar solicitação de estorno
 * - Validar regras de negócio (pagamento autorizado, prazo 24h, valor correto)
 * - Simular processamento de estorno
 * - Consultar estornos
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
    private final Random random = new Random();

    /**
     * Cria uma nova solicitação de estorno.
     * 
     * Validações:
     * 1. Pagamento existe
     * 2. Pagamento está AUTORIZADO
     * 3. Valor do estorno = valor do pagamento (estorno parcial não permitido)
     * 4. Dentro da janela de 24 horas
     * 5. Não existe estorno CANCELADO para este pagamento (constraint no banco)
     * 
     * @param request DTO com dados do estorno
     * @return DTO de resposta com status do estorno
     */
    @Transactional
    public EstornoResponseDTO criarEstorno(EstornoRequestDTO request) {
        log.info("Criando estorno para transação: {}", request.getIdTransacao());

        // 1. Buscar pagamento original
        Pagamento pagamento = pagamentoRepository.findByIdTransacao(request.getIdTransacao())
            .orElseThrow(() -> {
                log.warn("Pagamento não encontrado: {}", request.getIdTransacao());
                return new RecursoNaoEncontradoException("Pagamento", request.getIdTransacao());
            });

        // 2. Validar se pagamento está AUTORIZADO
        if (pagamento.getStatus() != StatusPagamento.AUTORIZADO) {
            log.warn("Tentativa de estornar pagamento com status {}: {}", 
                pagamento.getStatus(), request.getIdTransacao());
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
        OffsetDateTime agora = OffsetDateTime.now();
        Duration tempoDecorrido = Duration.between(pagamento.getDataHora(), agora);
        if (tempoDecorrido.toHours() > 24) {
            log.warn("Estorno solicitado após 24h. Pagamento: {}, Horas decorridas: {}", 
                request.getIdTransacao(), tempoDecorrido.toHours());
            throw new NegocioException(
                "Estorno só pode ser solicitado dentro de 24 horas. Tempo decorrido: " + 
                tempoDecorrido.toHours() + " horas"
            );
        }

        // 5. Verificar se já existe estorno CANCELADO para este pagamento
        boolean existeEstornoCancelado = repository.existsEstornoCanceladoByIdTransacaoPagamento(
            request.getIdTransacao()
        );
        if (existeEstornoCancelado) {
            log.warn("Já existe estorno CANCELADO para o pagamento: {}", request.getIdTransacao());
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

        // 7. Simular processamento do estorno
        StatusEstorno statusAnterior = estorno.getStatus();
        simularProcessamentoEstorno(estorno);

        // Salvar com novo status
        estorno = repository.save(estorno);

        // Publicar evento: Status Alterado (se mudou)
        if (!statusAnterior.equals(estorno.getStatus())) {
            publicarEventoStatusAlterado(estorno, pagamento, statusAnterior);
        }

        log.info("Estorno {} finalizado - Status: {}", 
            estorno.getIdEstorno(), estorno.getStatus());

        return mapper.paraDTO(estorno);
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

        return mapper.paraDTO(estorno);
    }

    /**
     * Lista estornos de um pagamento específico.
     * 
     * @param idTransacao UUID da transação do pagamento
     * @return Lista de DTOs de resposta
     */
    public List<EstornoResponseDTO> listarPorIdTransacao(String idTransacao) {
        log.info("Listando estornos do pagamento: {}", idTransacao);

        List<Estorno> estornos = repository.findByIdTransacaoPagamento(idTransacao);

        return estornos.stream()
            .map(mapper::paraDTO)
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
            .map(mapper::paraDTO)
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
            .map(mapper::paraDTO)
            .collect(Collectors.toList());
    }

    /**
     * Simula processamento do estorno com adquirente.
     * 
     * Regras de simulação:
     * - 95% de chance de CANCELADO (aprovado)
     * - 5% de chance de NEGADO
     * - Se cancelado: gera NSU e código de autorização
     * 
     * @param estorno Estorno a ser processado
     */
    private void simularProcessamentoEstorno(Estorno estorno) {
        log.info("Simulando processamento do estorno {}", estorno.getIdEstorno());

        // Simular resposta da adquirente (95% aprovação para estorno)
        boolean cancelado = random.nextInt(100) < 95;

        if (cancelado) {
            estorno.setStatus(StatusEstorno.CANCELADO);
            estorno.setNsu(gerarNSU());
            estorno.setCodigoAutorizacao(gerarCodigoAutorizacao());
            log.info("Estorno CANCELADO (aprovado) - NSU: {}, Código: {}", 
                estorno.getNsu(), estorno.getCodigoAutorizacao());
        } else {
            estorno.setStatus(StatusEstorno.NEGADO);
            log.warn("Estorno NEGADO - ID: {}", estorno.getIdEstorno());
        }
    }

    /**
     * Gera NSU (Número Sequencial Único) simulado.
     * Formato: 10 dígitos numéricos
     * 
     * @return NSU gerado
     */
    private String gerarNSU() {
        return String.format("%010d", random.nextInt(1000000000));
    }

    /**
     * Gera código de autorização simulado.
     * Formato: 6 dígitos numéricos
     * 
     * @return Código de autorização
     */
    private String gerarCodigoAutorizacao() {
        return String.format("%06d", random.nextInt(1000000));
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
}
