package br.com.sicredi.toolschallenge.pagamento.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Evento de domínio: Pagamento Criado.
 * 
 * Publicado quando um novo pagamento é criado no sistema.
 * Consumidores podem usar para:
 * - Notificar cliente
 * - Atualizar dashboards
 * - Acionar processos downstream
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoCriadoEvento {
    
    private Long idPagamento;
    private String idTransacao;
    private String descricao;
    private BigDecimal valor;
    private String metodoPagamento;
    private String formaPagamento;
    private String status;
    private OffsetDateTime criadoEm;
}
