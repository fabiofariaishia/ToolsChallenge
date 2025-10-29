package br.com.sicredi.toolschallenge.infra.outbox.repository;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Repository para o padrão Transactional Outbox.
 * 
 * Responsável por:
 * - Buscar eventos pendentes para publicação no Kafka
 * - Atualizar status após processamento
 * - Limpar eventos antigos processados
 * - Buscar eventos com erro para retry
 */
@Repository
public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, Long> {
    
    /**
     * Busca eventos pendentes para processamento.
     * Ordenados por data de criação (FIFO).
     * 
     * @return Lista de eventos pendentes
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.status = 'PENDENTE' ORDER BY o.criadoEm ASC")
    List<OutboxEvento> findEventosPendentes();
    
    /**
     * Busca eventos pendentes limitando a quantidade.
     * Usado para processamento em lote (batch).
     * 
     * @param limit Quantidade máxima de eventos
     * @return Lista de eventos pendentes
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.status = 'PENDENTE' ORDER BY o.criadoEm ASC LIMIT :limit")
    List<OutboxEvento> findEventosPendentes(@Param("limit") int limit);
    
    /**
     * Busca eventos com erro que ainda podem ser reprocessados.
     * Eventos com tentativas < 3 são elegíveis para retry.
     * 
     * @return Lista de eventos com erro para retry
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.status = 'ERRO' AND o.tentativas < 3 ORDER BY o.criadoEm ASC")
    List<OutboxEvento> findEventosParaRetry();
    
    /**
     * Busca eventos com erro que excederam o limite de tentativas.
     * Estes eventos precisam de intervenção manual.
     * 
     * @return Lista de eventos em DLQ (Dead Letter Queue)
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.status = 'ERRO' AND o.tentativas >= 3 ORDER BY o.criadoEm ASC")
    List<OutboxEvento> findEventosDLQ();
    
    /**
     * Busca eventos por tipo.
     * 
     * @param eventoTipo Tipo do evento (ex: "PagamentoAutorizado", "EstornoCancelado")
     * @return Lista de eventos do tipo especificado
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.eventoTipo = :eventoTipo")
    List<OutboxEvento> findByEventoTipo(@Param("eventoTipo") String eventoTipo);
    
    /**
     * Busca eventos por agregado (ex: buscar todos eventos de um pagamento específico).
     * 
     * @param agregadoId ID do agregado (ex: idTransacao do pagamento)
     * @return Lista de eventos do agregado
     */
    List<OutboxEvento> findByAgregadoId(String agregadoId);
    
    /**
     * Limpa eventos processados antigos (housekeeping).
     * Remove eventos processados há mais de N dias.
     * 
     * @param dataLimite Data limite (eventos antes desta data serão removidos)
     * @return Quantidade de eventos removidos
     */
    @Modifying
    @Query("DELETE FROM OutboxEvento o WHERE o.status = 'PROCESSADO' AND o.processadoEm < :dataLimite")
    int deleteEventosProcessadosAntigos(@Param("dataLimite") OffsetDateTime dataLimite);
    
    /**
     * Conta eventos pendentes (para monitoramento).
     * 
     * @return Quantidade de eventos pendentes
     */
    @Query("SELECT COUNT(o) FROM OutboxEvento o WHERE o.status = 'PENDENTE'")
    Long countEventosPendentes();
    
    /**
     * Conta eventos com erro (para alertas).
     * 
     * @return Quantidade de eventos com erro
     */
    @Query("SELECT COUNT(o) FROM OutboxEvento o WHERE o.status = 'ERRO'")
    Long countEventosComErro();
    
    /**
     * Busca eventos processados em um período (para auditoria).
     * 
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de eventos processados no período
     */
    @Query("SELECT o FROM OutboxEvento o WHERE o.status = 'PROCESSADO' AND o.processadoEm BETWEEN :dataInicio AND :dataFim")
    List<OutboxEvento> findEventosProcessadosNoPeriodo(
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
}
