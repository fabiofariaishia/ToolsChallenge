package br.com.sicredi.toolschallenge.pagamento.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Evento de domínio: Status de Pagamento Alterado.
 * 
 * Publicado quando o status de um pagamento muda.
 * Exemplos de transições:
 * - PENDENTE → AUTORIZADO
 * - PENDENTE → NEGADO
 * - AUTORIZADO → CANCELADO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoStatusAlteradoEvento {
    
    private Long idPagamento;
    private String idTransacao;
    private String statusAnterior;
    private String statusNovo;
    private OffsetDateTime alteradoEm;
    private String motivo;
}
