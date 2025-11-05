package br.com.sicredi.toolschallenge.estorno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para descrição de transação de estorno.
 * Contém valor, data/hora, estabelecimento, NSU, código de autorização e status.
 * 
 * @author ToolsChallenge Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Descrição detalhada da transação de estorno")
public class DescricaoEstornoDTO {

    /**
     * Valor da transação.
     */
    @Schema(
        description = "Valor da transação em reais",
        example = "50.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @DecimalMax(value = "999999.99", message = "Valor máximo é R$ 999.999,99")
    private BigDecimal valor;

    /**
     * Data e hora da transação (formato: dd/MM/yyyy HH:mm:ss).
     */
    @Schema(
        description = "Data e hora da transação no formato dd/MM/yyyy HH:mm:ss",
        example = "01/05/2021 18:30:00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Data/hora é obrigatória")
    @Pattern(
        regexp = "^\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}$",
        message = "Data/hora deve estar no formato dd/MM/yyyy HH:mm:ss"
    )
    private String dataHora;

    /**
     * Nome do estabelecimento comercial.
     */
    @Schema(
        description = "Nome do estabelecimento comercial onde foi realizada a compra",
        example = "PetShop Mundo Cão",
        minLength = 3,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Nome do estabelecimento é obrigatório")
    @Size(min = 3, max = 255, message = "Nome do estabelecimento deve ter entre 3 e 255 caracteres")
    private String estabelecimento;

    /**
     * NSU - Número Sequencial Único (retornado após autorização do estorno).
     * Presente apenas na response.
     */
    @Schema(
        description = "NSU - Número Sequencial Único da transação de estorno",
        example = "1234567890"
    )
    private String nsu;

    /**
     * Código de autorização (retornado pela adquirente).
     * Presente apenas na response.
     */
    @Schema(
        description = "Código de autorização da adquirente",
        example = "147258369"
    )
    private String codigoAutorizacao;

    /**
     * Status atual da transação de estorno.
     * Valores possíveis: CANCELADO, NEGADO, PENDENTE
     */
    @Schema(
        description = "Status da transação de estorno (CANCELADO/NEGADO/PENDENTE)",
        example = "CANCELADO"
    )
    private String status;
}
