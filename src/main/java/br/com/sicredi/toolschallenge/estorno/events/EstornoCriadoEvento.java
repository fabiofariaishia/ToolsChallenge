package br.com.sicredi.toolschallenge.estorno.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Evento de domínio: Estorno Criado.
 * 
 * Publicado quando uma solicitação de estorno é criada.
 * Consumidores podem usar para:
 * - Notificar cliente
 * - Acionar processos de análise
 * - Atualizar dashboards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstornoCriadoEvento {
    
    private Long idEstorno;
    private Long idPagamento;
    private String idTransacao;
    private BigDecimal valorEstorno;
    private String motivo;
    private String status;
    private OffsetDateTime criadoEm;
}
