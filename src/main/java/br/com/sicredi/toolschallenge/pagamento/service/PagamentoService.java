package br.com.sicredi.toolschallenge.pagamento.service;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoRequestDTO;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoResponseDTO;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoMapper;
import br.com.sicredi.toolschallenge.pagamento.events.PagamentoCriadoEvento;
import br.com.sicredi.toolschallenge.pagamento.events.PagamentoStatusAlteradoEvento;
import br.com.sicredi.toolschallenge.pagamento.repository.PagamentoRepository;
import br.com.sicredi.toolschallenge.infra.outbox.publisher.EventoPublisher;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import br.com.sicredi.toolschallenge.adquirente.service.AdquirenteService;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service para operações de negócio de Pagamento.
 * 
 * Responsabilidades:
 * - Criar novo pagamento (status=PENDENTE)
 * - Autorizar via AdquirenteService (Circuit Breaker, Retry, Bulkhead)
 * - Consultar pagamentos
 * - Validações de regras de negócio
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagamentoService {

    private final PagamentoRepository repository;
    private final PagamentoMapper mapper;
    private final EventoPublisher eventoPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final AdquirenteService adquirenteService;
    private final Random random = new Random();

    /**
     * Cria um novo pagamento e autoriza com adquirente.
     * 
     * Fluxo:
     * 1. Valida request
     * 2. Cria pagamento com status=PENDENTE
     * 3. Autoriza com AdquirenteService (Circuit Breaker + Retry + Bulkhead)
     * 4. Mapeia StatusAutorizacao → StatusPagamento
     * 5. Atualiza status e persiste
     * 6. Retorna DTO de resposta
     * 
     * Mapeamento de Status:
     * - AUTORIZADO → AUTORIZADO (com NSU/código)
     * - NEGADO → NEGADO (sem NSU/código)
     * - PENDENTE → PENDENTE (Circuit Breaker OPEN ou timeout, para reprocessamento)
     * 
     * @param request DTO com dados do pagamento
     * @return DTO de resposta com status autorizado, negado ou pendente
     */
    @Transactional
    public PagamentoResponseDTO criarPagamento(PagamentoRequestDTO request) {
        log.info("Criando novo pagamento - Valor: R$ {}, Parcelas: {}, Tipo: {}", 
            request.getValor(), request.getParcelas(), request.getTipoPagamento());

        // Converter DTO para entidade
        Pagamento pagamento = mapper.toEntity(request);
        
        // Gerar ID de transação único
        pagamento.setIdTransacao(UUID.randomUUID().toString());
        
        // Definir data/hora da transação
        pagamento.setDataHora(OffsetDateTime.now());
        
        // Status inicial
        pagamento.setStatus(StatusPagamento.PENDENTE);
        
        // Gerar Snowflake ID (simulado - em produção usar gerador distribuído)
        pagamento.setSnowflakeId(gerarSnowflakeId());
        
        // Salvar como PENDENTE
        pagamento = repository.save(pagamento);
        log.info("Pagamento criado com ID: {}, Status: PENDENTE", pagamento.getIdTransacao());
        
        // Publicar evento: Pagamento Criado
        publicarEventoPagamentoCriado(pagamento);
        
        // Autorizar com adquirente (Circuit Breaker + Retry + Bulkhead)
        StatusPagamento statusAnterior = pagamento.getStatus();
        autorizarComAdquirente(pagamento);
        
        // Salvar com novo status
        pagamento = repository.save(pagamento);
        
        // Publicar evento: Status Alterado (se mudou)
        if (!statusAnterior.equals(pagamento.getStatus())) {
            publicarEventoStatusAlterado(pagamento, statusAnterior);
        }
        
        log.info("Pagamento {} finalizado - Status: {}", 
            pagamento.getIdTransacao(), pagamento.getStatus());
        
        return mapper.toDTO(pagamento);
    }

    /**
     * Busca pagamento por ID de transação.
     * 
     * @param idTransacao UUID da transação
     * @return DTO de resposta ou exception se não encontrado
     */
    public PagamentoResponseDTO buscarPorIdTransacao(String idTransacao) {
        log.info("Buscando pagamento por ID: {}", idTransacao);
        
        Pagamento pagamento = repository.findByIdTransacao(idTransacao)
            .orElseThrow(() -> {
                log.warn("Pagamento não encontrado: {}", idTransacao);
                return new RecursoNaoEncontradoException("Pagamento", idTransacao);
            });
        
        return mapper.toDTO(pagamento);
    }

    /**
     * Lista todos os pagamentos (limitado aos últimos 100).
     * 
     * @return Lista de DTOs de resposta
     */
    public List<PagamentoResponseDTO> listarPagamentos() {
        log.info("Listando últimos pagamentos");
        
        List<Pagamento> pagamentos = repository.findUltimosPagamentos();
        
        return pagamentos.stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Lista pagamentos por status.
     * 
     * @param status Status do pagamento
     * @return Lista de DTOs de resposta
     */
    public List<PagamentoResponseDTO> listarPorStatus(StatusPagamento status) {
        log.info("Listando pagamentos por status: {}", status);
        
        List<Pagamento> pagamentos = repository.findByStatusOrderByCriadoEmDesc(status);
        
        return pagamentos.stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Autoriza pagamento com adquirente via AdquirenteService.
     * 
     * Aplica padrões de resiliência:
     * - Circuit Breaker: Protege contra falhas em cascata
     * - Retry: Tenta até 3x em caso de falha transitória
     * - Bulkhead: Isola pool de threads
     * 
     * Mapeamento de Status:
     * - StatusAutorizacao.AUTORIZADO → StatusPagamento.AUTORIZADO + NSU + Código
     * - StatusAutorizacao.NEGADO → StatusPagamento.NEGADO
     * - StatusAutorizacao.PENDENTE → StatusPagamento.PENDENTE (Circuit Breaker OPEN ou timeout)
     * 
     * Pagamentos PENDENTE devem ser reprocessados posteriormente via scheduler.
     * 
     * @param pagamento Pagamento a ser autorizado
     */
    private void autorizarComAdquirente(Pagamento pagamento) {
        log.info("Autorizando pagamento {} com adquirente", pagamento.getIdTransacao());
        
        try {
            // Criar request para adquirente
            AutorizacaoRequest request = new AutorizacaoRequest(
                pagamento.getCartaoMascarado(),
                "***", // CVV não armazenado (PCI-DSS compliance)
                "12/2030", // Validade simulada
                pagamento.getValor(),
                pagamento.getEstabelecimento()
            );
            
            // Chamar adquirente (Circuit Breaker + Retry + Bulkhead)
            AutorizacaoResponse response = adquirenteService.autorizarPagamento(request);
            
            // Mapear StatusAutorizacao → StatusPagamento
            if (response.status() == StatusAutorizacao.AUTORIZADO) {
                pagamento.setStatus(StatusPagamento.AUTORIZADO);
                pagamento.setNsu(response.nsu());
                pagamento.setCodigoAutorizacao(response.codigoAutorizacao());
                log.info("Pagamento AUTORIZADO - ID: {}, NSU: {}, Código: {}", 
                    pagamento.getIdTransacao(), response.nsu(), response.codigoAutorizacao());
                
            } else if (response.status() == StatusAutorizacao.NEGADO) {
                pagamento.setStatus(StatusPagamento.NEGADO);
                log.warn("Pagamento NEGADO - ID: {}", pagamento.getIdTransacao());
                
            } else if (response.status() == StatusAutorizacao.PENDENTE) {
                pagamento.setStatus(StatusPagamento.PENDENTE);
                log.warn("Pagamento PENDENTE (Circuit Breaker ou timeout) - ID: {}", 
                    pagamento.getIdTransacao());
            }
            
        } catch (Exception ex) {
            // Fallback: Manter PENDENTE se erro inesperado
            pagamento.setStatus(StatusPagamento.PENDENTE);
            log.error("Erro ao autorizar pagamento {} - Mantendo PENDENTE: {}", 
                pagamento.getIdTransacao(), ex.getMessage());
        }
    }

    /**
     * Gera Snowflake ID simulado (time-sortable unique ID).
     * 
     * Formato: timestamp + worker ID + sequence
     * 
     * @return Snowflake ID (15 dígitos para caber em BIGINT)
     */
    private Long gerarSnowflakeId() {
        // Simplificado: timestamp em milissegundos + random
        long timestamp = System.currentTimeMillis() % 100000000L; // 8 dígitos
        long randomPart = random.nextInt(10000000); // 7 dígitos
        return (timestamp * 10000000L) + randomPart;
    }

    /**
     * Publica evento de pagamento criado no outbox.
     * 
     * @param pagamento Pagamento criado
     */
    private void publicarEventoPagamentoCriado(Pagamento pagamento) {
        try {
            PagamentoCriadoEvento evento = PagamentoCriadoEvento.builder()
                    .idPagamento(pagamento.getId())
                    .idTransacao(pagamento.getIdTransacao())
                    .descricao(pagamento.getEstabelecimento())
                    .valor(pagamento.getValor())
                    .metodoPagamento(pagamento.getTipoPagamento().name())
                    .formaPagamento(pagamento.getTipoPagamento().name())
                    .status(pagamento.getStatus().name())
                    .criadoEm(pagamento.getDataHora())
                    .build();

            // Publicar para Outbox (Kafka)
            eventoPublisher.publicarPagamentoCriado(evento);
            
            // Publicar para Auditoria (Event Listener)
            eventPublisher.publishEvent(evento);
            
        } catch (Exception e) {
            log.error("Erro ao publicar evento de pagamento criado: {}", pagamento.getIdTransacao(), e);
            // Não propaga exception para não impactar fluxo principal
        }
    }

    /**
     * Publica evento de status alterado no outbox.
     * 
     * @param pagamento Pagamento atualizado
     * @param statusAnterior Status anterior
     */
    private void publicarEventoStatusAlterado(Pagamento pagamento, StatusPagamento statusAnterior) {
        try {
            PagamentoStatusAlteradoEvento evento = PagamentoStatusAlteradoEvento.builder()
                    .idPagamento(pagamento.getId())
                    .idTransacao(pagamento.getIdTransacao())
                    .statusAnterior(statusAnterior.name())
                    .statusNovo(pagamento.getStatus().name())
                    .alteradoEm(OffsetDateTime.now())
                    .motivo("Autorização processada")
                    .build();

            // Publicar para Outbox (Kafka)
            eventoPublisher.publicarPagamentoStatusAlterado(evento);
            
            // Publicar para Auditoria (Event Listener)
            eventPublisher.publishEvent(evento);
            
        } catch (Exception e) {
            log.error("Erro ao publicar evento de status alterado: {}", pagamento.getIdTransacao(), e);
            // Não propaga exception para não impactar fluxo principal
        }
    }
}
