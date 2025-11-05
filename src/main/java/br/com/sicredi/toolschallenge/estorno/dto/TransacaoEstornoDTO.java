package br.com.sicredi.toolschallenge.estorno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transação de estorno.
 * Representa a estrutura aninhada da resposta de estorno, conforme padrão da API.
 * 
 * Estrutura JSON:
 * {
 *   "transacao": {
 *     "cartao": "4444********1234",
 *     "id": "10002356890001",
 *     "descricao": { ... },
 *     "formaPagamento": { ... }
 *   }
 * }
 * 
 * @author ToolsChallenge Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Transação de estorno com dados aninhados")
public class TransacaoEstornoDTO {

    /**
     * Número do cartão mascarado (primeiros 4 e últimos 4 dígitos).
     */
    @Schema(
        description = "Número do cartão mascarado (primeiros 4 e últimos 4 dígitos visíveis)",
        example = "4444********1234",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Número do cartão é obrigatório")
    @Size(min = 16, max = 19, message = "Número do cartão deve ter entre 16 e 19 caracteres")
    private String cartao;

    /**
     * ID único da transação (formato: 14 dígitos).
     */
    @Schema(
        description = "ID único da transação (14 dígitos)",
        example = "10002356890001",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "ID da transação é obrigatório")
    @Size(min = 14, max = 14, message = "ID da transação deve ter 14 dígitos")
    private String id;

    /**
     * Descrição detalhada da transação (valor, data/hora, estabelecimento, nsu, código, status).
     */
    @Schema(
        description = "Descrição detalhada da transação",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Descrição é obrigatória")
    @Valid
    private DescricaoEstornoDTO descricao;

    /**
     * Forma de pagamento (tipo e parcelas).
     */
    @Schema(
        description = "Forma de pagamento",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Forma de pagamento é obrigatória")
    @Valid
    private FormaPagamentoEstornoDTO formaPagamento;
}
