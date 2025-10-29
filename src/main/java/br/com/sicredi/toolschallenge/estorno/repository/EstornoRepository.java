package br.com.sicredi.toolschallenge.estorno.repository;

import br.com.sicredi.toolschallenge.estorno.domain.Estorno;
import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações de persistência de Estornos.
 * 
 * Queries customizadas incluem:
 * - Busca por ID de transação do pagamento original
 * - Validação de estorno cancelado existente (regra de negócio: apenas 1 por pagamento)
 * - Busca por período
 */
@Repository
public interface EstornoRepository extends JpaRepository<Estorno, Long> {
    
    /**
     * Busca todos os estornos de um pagamento específico.
     * 
     * @param idTransacao UUID da transação do pagamento original
     * @return Lista de estornos do pagamento
     */
    @Query("SELECT e FROM Estorno e WHERE e.idTransacao = :idTransacao")
    List<Estorno> findByIdTransacaoPagamento(@Param("idTransacao") String idTransacao);
    
    /**
     * Busca estorno cancelado de um pagamento.
     * IMPORTANTE: Pela regra de negócio, só pode existir 1 estorno CANCELADO por pagamento.
     * 
     * @param idTransacao UUID da transação do pagamento original
     * @return Optional com o estorno cancelado se existir
     */
    @Query("SELECT e FROM Estorno e WHERE e.idTransacao = :idTransacao AND e.status = 'CANCELADO'")
    Optional<Estorno> findEstornoCanceladoByIdTransacaoPagamento(@Param("idTransacao") String idTransacao);
    
    /**
     * Verifica se já existe estorno cancelado para o pagamento.
     * Usado para validação antes de criar novo estorno.
     * 
     * @param idTransacao UUID da transação do pagamento original
     * @return true se já existe estorno cancelado
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Estorno e WHERE e.idTransacao = :idTransacao AND e.status = 'CANCELADO'")
    boolean existsEstornoCanceladoByIdTransacaoPagamento(@Param("idTransacao") String idTransacao);
    
    /**
     * Busca estornos por status.
     * 
     * @param status Status do estorno
     * @return Lista de estornos com o status informado
     */
    List<Estorno> findByStatus(StatusEstorno status);
    
    /**
     * Busca estornos por status ordenados por data de criação (mais recentes primeiro).
     * 
     * @param status Status do estorno
     * @return Lista ordenada de estornos
     */
    List<Estorno> findByStatusOrderByCriadoEmDesc(StatusEstorno status);
    
    /**
     * Busca estornos pendentes (para processamento assíncrono).
     * 
     * @return Lista de estornos pendentes
     */
    @Query("SELECT e FROM Estorno e WHERE e.status = 'PENDENTE' ORDER BY e.criadoEm ASC")
    List<Estorno> findEstornosPendentes();
    
    /**
     * Busca estornos criados em um período específico.
     * 
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de estornos no período
     */
    @Query("SELECT e FROM Estorno e WHERE e.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY e.criadoEm DESC")
    List<Estorno> findByPeriodo(
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Conta estornos por status em um período.
     * Útil para dashboards e métricas.
     * 
     * @param status Status do estorno
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Quantidade de estornos
     */
    @Query("SELECT COUNT(e) FROM Estorno e WHERE e.status = :status AND e.criadoEm BETWEEN :dataInicio AND :dataFim")
    Long countByStatusAndPeriodo(
        @Param("status") StatusEstorno status,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca estornos por NSU.
     * 
     * @param nsu Número sequencial único do estorno
     * @return Optional com o estorno se encontrado
     */
    Optional<Estorno> findByNsu(String nsu);
    
    /**
     * Busca os últimos N estornos (para monitoramento).
     * 
     * @return Lista dos estornos mais recentes
     */
    @Query("SELECT e FROM Estorno e ORDER BY e.criadoEm DESC LIMIT 100")
    List<Estorno> findUltimosEstornos();
}
