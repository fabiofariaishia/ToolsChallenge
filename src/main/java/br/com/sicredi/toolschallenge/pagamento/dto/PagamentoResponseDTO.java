package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO para resposta de pagamento.
 * 
 * Expõe informações relevantes para o cliente, ocultando campos internos.
 * Campos nulos são omitidos do JSON (@JsonInclude).
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
     * ID único da transação (chave de negócio).
     * UUID gerado pelo sistema.
     */
    @Schema(
        description = "Identificador único da transação (UUID)",
        example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private String idTransacao;

    /**
     * Status atual do pagamento.
     */
    @Schema(
        description = "Status atual do pagamento",
        example = "AUTORIZADO",
        allowableValues = {"PENDENTE", "AUTORIZADO", "NEGADO"}
    )
    private StatusPagamento status;

    /**
     * Valor da transação.
     */
    @Schema(
        description = "Valor da transação em reais",
        example = "150.50"
    )
    private BigDecimal valor;

    /**
     * Moeda da transação (ISO 4217).
     */
    @Schema(
        description = "Código da moeda no padrão ISO 4217",
        example = "BRL"
    )
    private String moeda;

    /**
     * Data/hora da transação (ISO 8601 com timezone).
     */
    @Schema(
        description = "Data e hora da transação no formato ISO 8601",
        example = "2025-11-04T14:30:00.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime dataHora;

    /**
     * Nome do estabelecimento.
     */
    @Schema(
        description = "Nome do estabelecimento comercial",
        example = "Supermercado Exemplo Ltda"
    )
    private String estabelecimento;

    /**
     * Tipo de pagamento.
     */
    @Schema(
        description = "Tipo de pagamento",
        example = "AVISTA"
    )
    private TipoPagamento tipoPagamento;

    /**
     * Número de parcelas.
     */
    @Schema(
        description = "Número de parcelas",
        example = "1"
    )
    private Integer parcelas;

    /**
     * NSU - Número Sequencial Único (retornado após autorização).
     * Presente apenas se status = AUTORIZADO.
     */
    @Schema(
        description = "NSU - Número Sequencial Único da transação (presente apenas se autorizado)",
        example = "123456789"
    )
    private String nsu;

    /**
     * Código de autorização (retornado pela adquirente).
     * Presente apenas se status = AUTORIZADO.
     */
    @Schema(
        description = "Código de autorização da adquirente (presente apenas se autorizado)",
        example = "AUTH987654"
    )
    private String codigoAutorizacao;

    /**
     * Cartão mascarado (PCI-DSS).
     */
    @Schema(
        description = "Número do cartão mascarado para segurança PCI-DSS",
        example = "4111********1111"
    )
    private String cartaoMascarado;

    /**
     * Data/hora de criação do registro.
     */
    @Schema(
        description = "Data e hora de criação do registro no formato ISO 8601",
        example = "2025-11-04T14:30:00.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime criadoEm;

    /**
     * Data/hora da última atualização.
     */
    @Schema(
        description = "Data e hora da última atualização no formato ISO 8601",
        example = "2025-11-04T14:30:05.000-03:00"
    )
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime atualizadoEm;

    /**
     * Mensagem adicional (ex: motivo de negação).
     * Presente apenas em casos especiais.
     */
    @Schema(
        description = "Mensagem adicional sobre o resultado da transação",
        example = "Transação autorizada com sucesso"
    )
    private String mensagem;
}
