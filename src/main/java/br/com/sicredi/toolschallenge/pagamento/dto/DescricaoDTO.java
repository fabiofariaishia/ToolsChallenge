package br.com.sicredi.toolschallenge.pagamento.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import br.com.sicredi.toolschallenge.shared.config.StringToBigDecimalDeserializer;
import br.com.sicredi.toolschallenge.shared.config.FlexibleDateTimeDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO representando a descrição da transação.
 * 
 * Contém: valor, dataHora e estabelecimento.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Schema(description = "Descrição detalhada da transação")
public class DescricaoDTO {

    /**
     * Valor da transação em reais.
     * Aceita String ou Number no JSON (ex: "500.50" ou 500.50).
     * Mínimo: R$ 0.01
     * Máximo: R$ 999.999,99
     */
    @Schema(
        description = "Valor da transação em reais (aceita String ou Number)",
        example = "500.50",
        minimum = "0.01",
        maximum = "999999.99",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    @DecimalMax(value = "999999.99", message = "Valor máximo é R$ 999.999,99")
    @Digits(integer = 6, fraction = 2, message = "Valor deve ter no máximo 6 dígitos inteiros e 2 decimais")
    @JsonDeserialize(using = StringToBigDecimalDeserializer.class)
    private BigDecimal valor;

    /**
     * Data/hora da transação.
     * Aceita múltiplos formatos:
     * - "dd/MM/yyyy HH:mm:ss" (ex: "01/05/2021 18:30:00")
     * - ISO 8601 (ex: "2021-05-01T18:30:00-03:00")
     */
    @Schema(
        description = "Data e hora da transação (aceita 'dd/MM/yyyy HH:mm:ss' ou ISO 8601)",
        example = "01/05/2021 18:30:00"
    )
    @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime dataHora;

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
}
