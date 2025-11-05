package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import br.com.sicredi.toolschallenge.shared.config.StringToIntegerDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO representando a forma de pagamento.
 * 
 * Contém: tipo e parcelas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Forma de pagamento da transação")
public class FormaPagamentoDTO {

    /**
     * Tipo de pagamento: AVISTA, PARCELADO_LOJA ou PARCELADO_EMISSOR.
     */
    @Schema(
        description = "Tipo de pagamento: AVISTA (1x), PARCELADO_LOJA (parcelado sem juros) ou PARCELADO_EMISSOR (parcelado com juros)",
        example = "AVISTA",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Tipo de pagamento é obrigatório")
    private TipoPagamento tipo;

    /**
     * Número de parcelas.
     * Aceita String ou Number no JSON (ex: "1" ou 1).
     * Mínimo: 1 (à vista)
     * Máximo: 12
     */
    @Schema(
        description = "Número de parcelas (1 para à vista, 2 a 12 para parcelado) - aceita String ou Number",
        example = "1",
        minimum = "1",
        maximum = "12",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número mínimo de parcelas é 1")
    @Max(value = 12, message = "Número máximo de parcelas é 12")
    @JsonDeserialize(using = StringToIntegerDeserializer.class)
    private Integer parcelas;

    /**
     * Validação customizada: parcelas devem ser 1 se tipo for AVISTA.
     */
    @AssertTrue(message = "Pagamento à vista deve ter apenas 1 parcela")
    public boolean isParcelasValidasParaAvista() {
        if (tipo == null) {
            return true; // Deixa @NotNull validar
        }
        if (tipo == TipoPagamento.AVISTA) {
            return parcelas == null || parcelas == 1;
        }
        return true;
    }

    /**
     * Validação customizada: parcelas devem ser > 1 se tipo for parcelado.
     */
    @AssertTrue(message = "Pagamento parcelado deve ter mais de 1 parcela")
    public boolean isParcelasValidasParaParcelado() {
        if (tipo == null || parcelas == null) {
            return true; // Deixa @NotNull validar
        }
        if (tipo == TipoPagamento.PARCELADO_LOJA || 
            tipo == TipoPagamento.PARCELADO_EMISSOR) {
            return parcelas > 1;
        }
        return true;
    }
}
