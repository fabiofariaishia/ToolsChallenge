package br.com.sicredi.toolschallenge.infra.auditoria.repository;

import br.com.sicredi.toolschallenge.infra.auditoria.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository para log de auditoria.
 * 
 * Responsável por:
 * - Registrar eventos do sistema para compliance
 * - Buscar histórico de eventos por agregado
 * - Queries para análise e troubleshooting
 * - Suporte a investigações de incidentes
 */
@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria, Long> {
    
    /**
     * Busca todos os eventos de auditoria de um agregado específico.
     * Ordenados cronologicamente (mais antigos primeiro).
     * 
     * @param agregadoId ID do agregado (ex: idTransacao do pagamento)
     * @return Lista de eventos do agregado
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.agregadoId = :agregadoId ORDER BY e.criadoEm ASC")
    List<EventoAuditoria> findByAgregadoId(@Param("agregadoId") String agregadoId);
    
    /**
     * Busca eventos de auditoria por tipo.
     * 
     * @param eventoTipo Tipo do evento (ex: "PagamentoAutorizado", "EstornoCancelado")
     * @return Lista de eventos do tipo especificado
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.eventoTipo = :eventoTipo")
    List<EventoAuditoria> findByEventoTipo(@Param("eventoTipo") String eventoTipo);
    
    /**
     * Busca eventos por tipo ordenados por data (mais recentes primeiro).
     * 
     * @param eventoTipo Tipo do evento
     * @return Lista ordenada de eventos
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.eventoTipo = :eventoTipo ORDER BY e.criadoEm DESC")
    List<EventoAuditoria> findByEventoTipoOrderByCriadoEmDesc(@Param("eventoTipo") String eventoTipo);
    
    /**
     * Busca eventos de auditoria em um período.
     * 
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de eventos no período
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY e.criadoEm DESC")
    List<EventoAuditoria> findByPeriodo(
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca eventos de um agregado em um período.
     * Útil para análise temporal de um pagamento específico.
     * 
     * @param agregadoId ID do agregado
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de eventos do agregado no período
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.agregadoId = :agregadoId AND e.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY e.criadoEm ASC")
    List<EventoAuditoria> findByAgregadoIdAndPeriodo(
        @Param("agregadoId") String agregadoId,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca eventos por tipo e agregado.
     * 
     * @param eventoTipo Tipo do evento
     * @param agregadoId ID do agregado
     * @return Lista de eventos
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.eventoTipo = :eventoTipo AND e.agregadoId = :agregadoId ORDER BY e.criadoEm ASC")
    List<EventoAuditoria> findByEventoTipoAndAgregadoId(
        @Param("eventoTipo") String eventoTipo,
        @Param("agregadoId") String agregadoId
    );
    
    /**
     * Busca os últimos N eventos (para monitoramento em tempo real).
     * 
     * @param limit Quantidade de eventos
     * @return Lista dos eventos mais recentes
     */
    @Query("SELECT e FROM EventoAuditoria e ORDER BY e.criadoEm DESC LIMIT :limit")
    List<EventoAuditoria> findUltimosEventos(@Param("limit") int limit);
    
    /**
     * Conta eventos por tipo em um período.
     * Útil para dashboards e métricas de compliance.
     * 
     * @param eventoTipo Tipo do evento
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Quantidade de eventos
     */
    @Query("SELECT COUNT(e) FROM EventoAuditoria e WHERE e.eventoTipo = :eventoTipo AND e.criadoEm BETWEEN :dataInicio AND :dataFim")
    Long countByEventoTipoAndPeriodo(
        @Param("eventoTipo") String eventoTipo,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca eventos por tipo de agregado (ex: todos eventos de pagamentos).
     * 
     * @param agregadoTipo Tipo do agregado (ex: "Pagamento", "Estorno")
     * @return Lista de eventos
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.agregadoTipo = :agregadoTipo")
    List<EventoAuditoria> findByAgregadoTipo(@Param("agregadoTipo") String agregadoTipo);
    
    /**
     * Busca eventos de um agregado específico ordenados cronologicamente.
     * Retorna histórico completo para event sourcing/auditoria.
     * 
     * @param agregadoTipo Tipo do agregado
     * @param agregadoId ID do agregado
     * @return Lista ordenada de eventos (história completa do agregado)
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.agregadoTipo = :agregadoTipo AND e.agregadoId = :agregadoId ORDER BY e.criadoEm ASC")
    List<EventoAuditoria> findHistoricoCompleto(
        @Param("agregadoTipo") String agregadoTipo,
        @Param("agregadoId") String agregadoId
    );
}
