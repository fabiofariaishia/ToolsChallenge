package br.com.sicredi.toolschallenge.estorno.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO para requisição de criação de estorno.
 * 
 * Validações aplicadas:
 * - ID da transação obrigatório (UUID do pagamento)
 * - Valor deve bater com o valor do pagamento original
 * - Motivo opcional mas recomendado
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Requisição para criar estorno de um pagamento autorizado")
public class EstornoRequestDTO {

    /**
     * ID da transação do pagamento a ser estornado.
     * Deve ser um UUID válido de um pagamento AUTORIZADO.
     */
    @Schema(
        description = "Identificador único da transação do pagamento a ser estornado (UUID)",
        example = "123e4567-e89b-12d3-a456-426614174000",
        minLength = 36,
        maxLength = 50,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "ID da transação é obrigatório")
    @Size(min = 36, max = 50, message = "ID da transação deve ter entre 36 e 50 caracteres")
    private String idTransacao;

    /**
     * Valor a ser estornado.
     * DEVE ser igual ao valor total do pagamento original.
     * Estorno parcial não é permitido.
     */
    @Schema(
        description = "Valor a ser estornado (deve ser igual ao valor total do pagamento original, estorno parcial não permitido)",
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
     * Motivo do estorno (opcional mas recomendado).
     * Exemplos: "Produto não entregue", "Cliente solicitou cancelamento", etc.
     */
    @Schema(
        description = "Motivo do estorno (opcional mas recomendado para auditoria)",
        example = "Cliente solicitou cancelamento da compra",
        maxLength = 500
    )
    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    private String motivo;
}
