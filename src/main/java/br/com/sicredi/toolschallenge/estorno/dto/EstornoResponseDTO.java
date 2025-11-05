package br.com.sicredi.toolschallenge.estorno.dto;

import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para resposta de estorno.
 * 
 * Expõe informações relevantes para o cliente.
 * Campos nulos são omitidos do JSON.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta com dados do estorno processado")
public class EstornoResponseDTO {

    /**
     * ID da transação original do pagamento.
     */
    @Schema(
        description = "Identificador único da transação original do pagamento (UUID)",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String idTransacao;

    /**
     * ID único do estorno (gerado pelo sistema).
     */
    @Schema(
        description = "Identificador único do estorno (UUID)",
        example = "987fcdeb-51a2-43f6-b789-012345678901"
    )
    private String idEstorno;

    /**
     * Status atual do estorno.
     */
    @Schema(
        description = "Status atual do estorno",
        example = "CANCELADO",
        allowableValues = {"PENDENTE", "CANCELADO", "NEGADO"}
    )
    private StatusEstorno status;

    /**
     * Valor estornado.
     */
    @Schema(
        description = "Valor estornado em reais",
        example = "150.50"
    )
    private BigDecimal valor;

    /**
     * Data/hora da solicitação do estorno (ISO 8601 com timezone).
     */
    @Schema(
        description = "Data e hora da solicitação do estorno no formato ISO 8601",
        example = "2025-11-04T15:30:00.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime dataHora;

    /**
     * NSU do estorno (retornado após cancelamento).
     * Presente apenas se status = CANCELADO.
     */
    @Schema(
        description = "NSU - Número Sequencial Único do estorno (presente apenas se cancelado)",
        example = "987654321"
    )
    private String nsu;

    /**
     * Código de autorização do estorno.
     * Presente apenas se status = CANCELADO.
     */
    @Schema(
        description = "Código de autorização do estorno (presente apenas se cancelado)",
        example = "CANCEL123456"
    )
    private String codigoAutorizacao;

    /**
     * Motivo do estorno fornecido pelo solicitante.
     */
    @Schema(
        description = "Motivo do estorno informado na solicitação",
        example = "Cliente solicitou cancelamento da compra"
    )
    private String motivo;

    /**
     * Data/hora de criação do registro.
     */
    @Schema(
        description = "Data e hora de criação do registro no formato ISO 8601",
        example = "2025-11-04T15:30:00.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime criadoEm;

    /**
     * Data/hora da última atualização.
     */
    @Schema(
        description = "Data e hora da última atualização no formato ISO 8601",
        example = "2025-11-04T15:30:05.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime atualizadoEm;

    /**
     * Mensagem adicional (ex: motivo de negação).
     */
    @Schema(
        description = "Mensagem adicional sobre o resultado do estorno",
        example = "Estorno processado com sucesso"
    )
    private String mensagem;
}
