package br.com.sicredi.toolschallenge.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * DTO para resposta de pagamento.
 * 
 * Retorna a transação processada com todos os dados relevantes
 * (incluindo NSU, código de autorização e status após processamento).
 * 
 * Estrutura: { "transacao": { ... } }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta com dados do pagamento processado")
public class PagamentoResponseDTO {

    /**
     * Dados completos da transação processada.
     * Inclui NSU, código de autorização e status.
     */
    @Schema(
        description = "Dados completos da transação processada",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Transação é obrigatória")
    @Valid
    private TransacaoDTO transacao;
}
