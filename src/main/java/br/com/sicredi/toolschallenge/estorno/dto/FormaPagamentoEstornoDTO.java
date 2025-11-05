package br.com.sicredi.toolschallenge.estorno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para forma de pagamento da transação de estorno.
 * Contém tipo (AVISTA/PARCELADO_LOJA/PARCELADO_EMISSOR) e número de parcelas.
 * 
 * @author ToolsChallenge Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Forma de pagamento da transação de estorno")
public class FormaPagamentoEstornoDTO {

    /**
     * Tipo de pagamento (AVISTA, PARCELADO_LOJA, PARCELADO_EMISSOR).
     */
    @Schema(
        description = "Tipo de pagamento",
        example = "AVISTA",
        allowableValues = {"AVISTA", "PARCELADO_LOJA", "PARCELADO_EMISSOR"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Tipo de pagamento é obrigatório")
    private String tipo;

    /**
     * Número de parcelas (1 para AVISTA, 2-12 para parcelado).
     */
    @Schema(
        description = "Número de parcelas (1 para AVISTA, 2-12 para parcelado)",
        example = "1",
        minimum = "1",
        maximum = "12",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número de parcelas deve ser no mínimo 1")
    private Integer parcelas;
}
