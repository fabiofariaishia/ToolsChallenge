package br.com.sicredi.toolschallenge.infra.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * Busca eventos por tipo de agregado e ID ordenados por data (mais recentes primeiro)
     */
    List<EventoAuditoria> findByAgregadoTipoAndAgregadoIdOrderByCriadoEmDesc(
            String agregadoTipo, 
            String agregadoId
    );

    /**
     * Busca eventos por tipo de agregado
     */
    List<EventoAuditoria> findByAgregadoTipoOrderByCriadoEmDesc(String agregadoTipo);

    /**
     * Busca eventos por tipo de evento
     */
    List<EventoAuditoria> findByEventoTipoOrderByCriadoEmDesc(String eventoTipo);    /**
     * Busca eventos em um período
     */
    @Query("SELECT e FROM EventoAuditoria e WHERE e.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY e.criadoEm DESC")
    List<EventoAuditoria> findByPeriodo(
            @Param("dataInicio") OffsetDateTime dataInicio,
            @Param("dataFim") OffsetDateTime dataFim
    );

    /**
     * Busca eventos por origem
     */
    List<EventoAuditoria> findByUsuarioOrderByCriadoEmDesc(String usuario);

    /**
     * Conta eventos por tipo de agregado
     */
    @Query("SELECT COUNT(e) FROM EventoAuditoria e WHERE e.agregadoTipo = :agregadoTipo")
    Long countByAgregadoTipo(@Param("agregadoTipo") String agregadoTipo);

    /**
     * Busca últimos eventos
     */
    @Query("SELECT e FROM EventoAuditoria e ORDER BY e.criadoEm DESC")
    List<EventoAuditoria> findUltimosEventos(org.springframework.data.domain.Pageable pageable);

    /**
     * Remove eventos antigos (para limpeza periódica)
     */
    @Modifying
    @Query("DELETE FROM EventoAuditoria e WHERE e.criadoEm < :dataLimite")
    void deleteEventosAntigos(@Param("dataLimite") OffsetDateTime dataLimite);
}