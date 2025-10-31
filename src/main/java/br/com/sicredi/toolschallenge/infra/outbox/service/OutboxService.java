package br.com.sicredi.toolschallenge.infra.outbox.service;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import br.com.sicredi.toolschallenge.infra.outbox.repository.OutboxEventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service para gerenciar eventos do Outbox Pattern.
 * 
 * Responsabilidades:
 * - Criar novos eventos na tabela outbox (dentro de transação)
 * - Buscar eventos pendentes para processamento
 * - Marcar eventos como processados ou com erro
 * - Limpar eventos processados antigos
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventoRepository repository;

    /**
     * Cria um novo evento no outbox.
     * Deve ser chamado dentro da mesma transação que a mudança de estado.
     * 
     * @param agregadoId ID do agregado (ex: ID do pagamento)
     * @param agregadoTipo Tipo do agregado (ex: "Pagamento", "Estorno")
     * @param eventoTipo Tipo do evento (ex: "PagamentoAutorizado", "EstornoCancelado")
     * @param payload Dados do evento
     * @param topicoKafka Tópico Kafka de destino
     * @return Evento criado
     */
    @Transactional
    public OutboxEvento criarEvento(
            String agregadoId,
            String agregadoTipo,
            String eventoTipo,
            Map<String, Object> payload,
            String topicoKafka) {
        
        OutboxEvento evento = OutboxEvento.builder()
                .agregadoId(agregadoId)
                .agregadoTipo(agregadoTipo)
                .eventoTipo(eventoTipo)
                .payload(payload)
                .topicoKafka(topicoKafka)
                .status("PENDENTE")
                .tentativas(0)
                .build();

        OutboxEvento salvo = repository.save(evento);
        log.debug("Evento criado no outbox: id={}, tipo={}, agregado={}", 
                salvo.getId(), eventoTipo, agregadoId);
        
        return salvo;
    }

    /**
     * Busca eventos pendentes para processamento.
     * 
     * @return Lista de eventos pendentes
     */
    @Transactional(readOnly = true)
    public List<OutboxEvento> buscarEventosPendentes() {
        return repository.findEventosPendentes();
    }

    /**
     * Busca eventos pendentes com limite de quantidade.
     * 
     * @param limit Quantidade máxima de eventos
     * @return Lista de eventos pendentes
     */
    @Transactional(readOnly = true)
    public List<OutboxEvento> buscarEventosPendentes(int limit) {
        return repository.findEventosPendentes(limit);
    }

    /**
     * Busca eventos com erro elegíveis para retry.
     * 
     * @return Lista de eventos para retry
     */
    @Transactional(readOnly = true)
    public List<OutboxEvento> buscarEventosParaRetry() {
        return repository.findEventosParaRetry();
    }

    /**
     * Marca evento como processado.
     * 
     * @param eventoId ID do evento
     */
    @Transactional
    public void marcarComoProcessado(Long eventoId) {
        repository.findById(eventoId).ifPresent(evento -> {
            evento.marcarComoProcessado();
            repository.save(evento);
            log.info("Evento marcado como processado: id={}, tipo={}", 
                    evento.getId(), evento.getEventoTipo());
        });
    }

    /**
     * Marca evento como erro.
     * 
     * @param eventoId ID do evento
     * @param mensagemErro Mensagem de erro
     */
    @Transactional
    public void marcarComoErro(Long eventoId, String mensagemErro) {
        repository.findById(eventoId).ifPresent(evento -> {
            evento.marcarComoErro(mensagemErro);
            repository.save(evento);
            log.warn("Evento marcado como erro: id={}, tipo={}, tentativas={}, erro={}", 
                    evento.getId(), evento.getEventoTipo(), evento.getTentativas(), mensagemErro);
        });
    }

    /**
     * Limpa eventos processados antigos.
     * Remove eventos processados há mais de N dias.
     * 
     * @param diasRetencao Dias de retenção
     * @return Quantidade de eventos removidos
     */
    @Transactional
    public int limparEventosProcessadosAntigos(int diasRetencao) {
        OffsetDateTime dataLimite = OffsetDateTime.now().minusDays(diasRetencao);
        int removidos = repository.deleteEventosProcessadosAntigos(dataLimite);
        
        if (removidos > 0) {
            log.info("Removidos {} eventos processados há mais de {} dias", removidos, diasRetencao);
        }
        
        return removidos;
    }

    /**
     * Busca eventos de um agregado específico (para auditoria).
     * 
     * @param agregadoId ID do agregado
     * @return Lista de eventos do agregado
     */
    @Transactional(readOnly = true)
    public List<OutboxEvento> buscarEventosPorAgregado(String agregadoId) {
        return repository.findByAgregadoId(agregadoId);
    }

    /**
     * Conta eventos pendentes (para monitoramento).
     * 
     * @return Quantidade de eventos pendentes
     */
    @Transactional(readOnly = true)
    public Long contarEventosPendentes() {
        return repository.countEventosPendentes();
    }

    /**
     * Conta eventos com erro (para alertas).
     * 
     * @return Quantidade de eventos com erro
     */
    @Transactional(readOnly = true)
    public Long contarEventosComErro() {
        return repository.countEventosComErro();
    }
}
