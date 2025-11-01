package br.com.sicredi.toolschallenge.infra.auditoria;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Serviço para gerenciamento de eventos de auditoria.
 * Registra todas as operações importantes do sistema para rastreabilidade.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private final EventoAuditoriaRepository repository;

    /**
     * Registra um evento de auditoria de forma assíncrona.
     * Usa propagação REQUIRES_NEW para garantir que a auditoria seja salva
     * mesmo se a transação principal falhar.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEvento(
            String agregadoTipo,
            String agregadoId,
            String eventoTipo,
            Map<String, Object> payload
    ) {
        try {
            EventoAuditoria evento = EventoAuditoria.builder()
                    .agregadoTipo(agregadoTipo)
                    .agregadoId(agregadoId)
                    .eventoTipo(eventoTipo)
                    .dados(payload)
                    .criadoEm(OffsetDateTime.now())
                    .usuario("SISTEMA")
                    .build();

            repository.save(evento);
            
            log.debug("Evento de auditoria registrado: tipo={}, agregado={}/{}", 
                    eventoTipo, agregadoTipo, agregadoId);
                    
        } catch (Exception e) {
            // Não propaga exceção para não impactar o fluxo principal
            log.error("Erro ao registrar evento de auditoria: {}", e.getMessage(), e);
        }
    }

    /**
     * Registra evento com metadados adicionais
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEventoComMetadados(
            String agregadoTipo,
            String agregadoId,
            String eventoTipo,
            Map<String, Object> payload,
            Map<String, Object> metadados
    ) {
        try {
            EventoAuditoria evento = EventoAuditoria.builder()
                    .agregadoTipo(agregadoTipo)
                    .agregadoId(agregadoId)
                    .eventoTipo(eventoTipo)
                    .dados(payload)
                    .metadados(metadados)
                    .criadoEm(OffsetDateTime.now())
                    .usuario("SISTEMA")
                    .build();

            repository.save(evento);
            
            log.debug("Evento de auditoria registrado com metadados: tipo={}, agregado={}/{}", 
                    eventoTipo, agregadoTipo, agregadoId);
                    
        } catch (Exception e) {
            log.error("Erro ao registrar evento de auditoria: {}", e.getMessage(), e);
        }
    }

    /**
     * Busca histórico de auditoria de um agregado
     */
    @Transactional(readOnly = true)
    public List<EventoAuditoria> buscarHistorico(String agregadoTipo, String agregadoId) {
        return repository.findByAgregadoTipoAndAgregadoIdOrderByCriadoEmDesc(agregadoTipo, agregadoId);
    }

    /**
     * Busca eventos por tipo
     */
    @Transactional(readOnly = true)
    public List<EventoAuditoria> buscarPorTipoEvento(String eventoTipo) {
        return repository.findByEventoTipoOrderByCriadoEmDesc(eventoTipo);
    }

    /**
     * Busca eventos em um período
     */
    @Transactional(readOnly = true)
    public List<EventoAuditoria> buscarPorPeriodo(OffsetDateTime dataInicio, OffsetDateTime dataFim) {
        return repository.findByPeriodo(dataInicio, dataFim);
    }

    /**
     * Busca últimos eventos
     */
    @Transactional(readOnly = true)
    public List<EventoAuditoria> buscarUltimosEventos(int quantidade) {
        return repository.findUltimosEventos(PageRequest.of(0, quantidade));
    }

    /**
     * Estatísticas de auditoria
     */
    @Transactional(readOnly = true)
    public Map<String, Long> obterEstatisticas() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalEventos", repository.count());
        stats.put("eventosPagamento", repository.countByAgregadoTipo("PAGAMENTO"));
        stats.put("eventosEstorno", repository.countByAgregadoTipo("ESTORNO"));
        return stats;
    }

    /**
     * Limpeza de eventos antigos (executado periodicamente)
     */
    @Transactional
    public void limparEventosAntigos(int diasRetencao) {
        OffsetDateTime dataLimite = OffsetDateTime.now().minusDays(diasRetencao);
        repository.deleteEventosAntigos(dataLimite);
        log.info("Eventos de auditoria anteriores a {} foram removidos", dataLimite);
    }
}
