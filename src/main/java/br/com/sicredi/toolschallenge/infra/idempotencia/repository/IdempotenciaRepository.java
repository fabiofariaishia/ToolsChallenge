package br.com.sicredi.toolschallenge.infra.idempotencia.repository;

import br.com.sicredi.toolschallenge.infra.idempotencia.Idempotencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository para controle de idempotência.
 * 
 * Responsável por:
 * - Verificar se uma requisição já foi processada
 * - Armazenar chaves de idempotência
 * - Limpar chaves expiradas (TTL 24h)
 * - Fallback para Redis (cache distribuído)
 */
@Repository
public interface IdempotenciaRepository extends JpaRepository<Idempotencia, String> {
    
    /**
     * Busca registro de idempotência por chave.
     * Chave é tipicamente gerada pelo cliente (UUID ou hash da requisição).
     * 
     * @param chave Chave de idempotência
     * @return Optional com o registro se encontrado
     */
    Optional<Idempotencia> findByChave(String chave);
    
    /**
     * Verifica se existe registro de idempotência válido (não expirado).
     * 
     * @param chave Chave de idempotência
     * @param now Data/hora atual para comparação
     * @return true se existe e ainda é válido
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Idempotencia i WHERE i.chave = :chave AND i.expiraEm > :now")
    boolean existsChaveValida(@Param("chave") String chave, @Param("now") OffsetDateTime now);
    
    /**
     * Busca registro de idempotência válido (não expirado).
     * Retorna a resposta armazenada para retornar ao cliente.
     * 
     * @param chave Chave de idempotência
     * @param now Data/hora atual para comparação
     * @return Optional com o registro se válido
     */
    @Query("SELECT i FROM Idempotencia i WHERE i.chave = :chave AND i.expiraEm > :now")
    Optional<Idempotencia> findChaveValida(@Param("chave") String chave, @Param("now") OffsetDateTime now);
    
    /**
     * Limpa registros expirados (housekeeping).
     * Executado por job agendado (ex: a cada 1 hora).
     * TTL padrão: 24 horas.
     * 
     * @param now Data/hora atual
     * @return Quantidade de registros removidos
     */
    @Modifying
    @Query("DELETE FROM Idempotencia i WHERE i.expiraEm < :now")
    int deleteChavesExpiradas(@Param("now") OffsetDateTime now);
    
    /**
     * Conta chaves expiradas (para monitoramento antes da limpeza).
     * 
     * @param now Data/hora atual
     * @return Quantidade de chaves expiradas
     */
    @Query("SELECT COUNT(i) FROM Idempotencia i WHERE i.expiraEm < :now")
    Long countChavesExpiradas(@Param("now") OffsetDateTime now);
    
    /**
     * Conta total de chaves ativas (não expiradas).
     * Útil para métricas e monitoramento.
     * 
     * @param now Data/hora atual
     * @return Quantidade de chaves ativas
     */
    @Query("SELECT COUNT(i) FROM Idempotencia i WHERE i.expiraEm > :now")
    Long countChavesAtivas(@Param("now") OffsetDateTime now);
    
    /**
     * Busca chaves criadas em um período (para auditoria).
     * 
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de registros criados no período
     */
    @Query("SELECT i FROM Idempotencia i WHERE i.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY i.criadoEm DESC")
    java.util.List<Idempotencia> findByPeriodo(
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
}
