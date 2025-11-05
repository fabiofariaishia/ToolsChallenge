package br.com.sicredi.toolschallenge.pagamento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para requisição de criação de pagamento.
 * 
 * Estrutura aninhada conforme padrão da adquirente:
 * - transacao: dados completos da transação (cartão, descricao, formaPagamento)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Requisição para criar novo pagamento com cartão")
public class PagamentoRequestDTO {

    /**
     * Dados completos da transação.
     */
    @Schema(
        description = "Dados completos da transação de pagamento",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Transação é obrigatória")
    @Valid
    private TransacaoDTO transacao;
}
