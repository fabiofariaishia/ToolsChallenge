package br.com.sicredi.toolschallenge.estorno.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Evento de domínio: Status de Estorno Alterado.
 * 
 * Publicado quando o status de um estorno muda.
 * Exemplos de transições:
 * - PENDENTE → APROVADO
 * - PENDENTE → NEGADO
 * - APROVADO → PROCESSADO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstornoStatusAlteradoEvento {
    
    private Long idEstorno;
    private Long idPagamento;
    private String idTransacao;
    private String statusAnterior;
    private String statusNovo;
    private OffsetDateTime alteradoEm;
    private String motivo;
}
