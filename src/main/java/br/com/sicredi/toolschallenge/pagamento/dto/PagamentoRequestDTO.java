package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para requisição de criação de pagamento.
 * 
 * Validações aplicadas:
 * - Valor mínimo: R$ 0.01
 * - Valor máximo: R$ 999.999,99
 * - Parcelas: 1 a 12
 * - Cartão mascarado obrigatório
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "cartaoMascarado") // Não logar dados sensíveis
@Schema(description = "Requisição para criar novo pagamento com cartão")
public class PagamentoRequestDTO {

    /**
     * Valor da transação em reais.
     * Mínimo: R$ 0.01
     * Máximo: R$ 999.999,99
     */
    @Schema(
        description = "Valor da transação em reais",
        example = "150.50",
        minimum = "0.01",
        maximum = "999999.99",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @DecimalMax(value = "999999.99", message = "Valor máximo é R$ 999.999,99")
    @Digits(integer = 6, fraction = 2, message = "Valor deve ter no máximo 6 dígitos inteiros e 2 decimais")
    private BigDecimal valor;

    /**
     * Moeda da transação (ISO 4217).
     * Padrão: BRL
     */
    @Schema(
        description = "Código da moeda no padrão ISO 4217",
        example = "BRL",
        defaultValue = "BRL",
        pattern = "^[A-Z]{3}$",
        minLength = 3,
        maxLength = 3
    )
    @Size(min = 3, max = 3, message = "Moeda deve ter exatamente 3 caracteres")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda deve estar em formato ISO 4217 (ex: BRL, USD)")
    @Builder.Default
    private String moeda = "BRL";

    /**
     * Nome do estabelecimento comercial.
     */
    @Schema(
        description = "Nome do estabelecimento comercial onde foi realizada a compra",
        example = "Supermercado Exemplo Ltda",
        minLength = 3,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Nome do estabelecimento é obrigatório")
    @Size(min = 3, max = 255, message = "Nome do estabelecimento deve ter entre 3 e 255 caracteres")
    private String estabelecimento;

    /**
     * Tipo de pagamento: AVISTA, PARCELADO_LOJA ou PARCELADO_EMISSOR.
     */
    @Schema(
        description = "Tipo de pagamento: AVISTA (1x), PARCELADO_LOJA (parcelado sem juros) ou PARCELADO_EMISSOR (parcelado com juros)",
        example = "AVISTA",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Tipo de pagamento é obrigatório")
    private TipoPagamento tipoPagamento;

    /**
     * Número de parcelas.
     * Mínimo: 1 (à vista)
     * Máximo: 12
     */
    @Schema(
        description = "Número de parcelas (1 para à vista, 2 a 12 para parcelado)",
        example = "1",
        minimum = "1",
        maximum = "12",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número mínimo de parcelas é 1")
    @Max(value = 12, message = "Número máximo de parcelas é 12")
    private Integer parcelas;

    /**
     * Número do cartão mascarado (PCI-DSS compliant).
     * Formato esperado: 4 primeiros + * + 4 últimos dígitos
     * Exemplo: "4111********1111"
     */
    @Schema(
        description = "Número do cartão mascarado para segurança PCI-DSS (primeiros 4 + ******** + últimos 4 dígitos)",
        example = "4111********1111",
        pattern = "^\\d{4}\\*{8}\\d{4}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Número do cartão mascarado é obrigatório")
    @Pattern(
        regexp = "^\\d{4}\\*{8}\\d{4}$",
        message = "Cartão mascarado deve estar no formato: 4111********1111"
    )
    private String cartaoMascarado;

    /**
     * Validação customizada: parcelas devem ser 1 se tipo for AVISTA.
     */
    @AssertTrue(message = "Pagamento à vista deve ter apenas 1 parcela")
    public boolean isParcelasValidasParaAvista() {
        if (tipoPagamento == null) {
            return true; // Deixa @NotNull validar
        }
        if (tipoPagamento == TipoPagamento.AVISTA) {
            return parcelas == null || parcelas == 1;
        }
        return true;
    }

    /**
     * Validação customizada: parcelas devem ser > 1 se tipo for parcelado.
     */
    @AssertTrue(message = "Pagamento parcelado deve ter mais de 1 parcela")
    public boolean isParcelasValidasParaParcelado() {
        if (tipoPagamento == null || parcelas == null) {
            return true; // Deixa @NotNull validar
        }
        if (tipoPagamento == TipoPagamento.PARCELADO_LOJA || 
            tipoPagamento == TipoPagamento.PARCELADO_EMISSOR) {
            return parcelas > 1;
        }
        return true;
    }
}
