package br.com.sicredi.toolschallenge.pagamento.repository;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações de persistência de Pagamentos.
 * 
 * Queries customizadas incluem:
 * - Busca por ID de transação (chave de negócio)
 * - Busca por NSU (identificador único da transação)
 * - Busca por status
 * - Busca por período
 */
@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    
    /**
     * Busca pagamento por ID de transação (chave de negócio única).
     * 
     * @param idTransacao UUID da transação
     * @return Optional com o pagamento se encontrado
     */
    Optional<Pagamento> findByIdTransacao(String idTransacao);
    
    /**
     * Verifica se existe pagamento com o ID de transação informado.
     * Útil para validações de duplicidade.
     * 
     * @param idTransacao UUID da transação
     * @return true se existe
     */
    boolean existsByIdTransacao(String idTransacao);
    
    /**
     * Busca pagamento por NSU (Número Sequencial Único).
     * NSU é gerado pela adquirente/autorizador.
     * 
     * @param nsu Número sequencial único
     * @return Optional com o pagamento se encontrado
     */
    Optional<Pagamento> findByNsu(String nsu);
    
    /**
     * Busca pagamentos por status.
     * 
     * @param status Status do pagamento
     * @return Lista de pagamentos com o status informado
     */
    List<Pagamento> findByStatus(StatusPagamento status);
    
    /**
     * Busca pagamentos por status ordenados por data de criação (mais recentes primeiro).
     * 
     * @param status Status do pagamento
     * @return Lista ordenada de pagamentos
     */
    List<Pagamento> findByStatusOrderByCriadoEmDesc(StatusPagamento status);
    
    /**
     * Busca pagamentos criados em um período específico.
     * 
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Lista de pagamentos no período
     */
    @Query("SELECT p FROM Pagamento p WHERE p.criadoEm BETWEEN :dataInicio AND :dataFim ORDER BY p.criadoEm DESC")
    List<Pagamento> findByPeriodo(
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca pagamentos autorizados (para processamento de estornos).
     * 
     * @return Lista de pagamentos autorizados
     */
    @Query("SELECT p FROM Pagamento p WHERE p.status = 'AUTORIZADO' ORDER BY p.criadoEm DESC")
    List<Pagamento> findPagamentosAutorizados();
    
    /**
     * Busca pagamentos autorizados por ID de transação (validação para estorno).
     * 
     * @param idTransacao UUID da transação
     * @return Optional com o pagamento autorizado se encontrado
     */
    @Query("SELECT p FROM Pagamento p WHERE p.idTransacao = :idTransacao AND p.status = 'AUTORIZADO'")
    Optional<Pagamento> findPagamentoAutorizadoByIdTransacao(@Param("idTransacao") String idTransacao);
    
    /**
     * Conta pagamentos por status em um período.
     * Útil para dashboards e métricas.
     * 
     * @param status Status do pagamento
     * @param dataInicio Data/hora de início
     * @param dataFim Data/hora de fim
     * @return Quantidade de pagamentos
     */
    @Query("SELECT COUNT(p) FROM Pagamento p WHERE p.status = :status AND p.criadoEm BETWEEN :dataInicio AND :dataFim")
    Long countByStatusAndPeriodo(
        @Param("status") StatusPagamento status,
        @Param("dataInicio") OffsetDateTime dataInicio,
        @Param("dataFim") OffsetDateTime dataFim
    );
    
    /**
     * Busca os últimos N pagamentos (para monitoramento).
     * 
     * @return Lista dos pagamentos mais recentes
     */
    @Query("SELECT p FROM Pagamento p ORDER BY p.criadoEm DESC LIMIT 100")
    List<Pagamento> findUltimosPagamentos();
}
