package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class PagamentoResponseDTO {

    /**
     * ID único da transação (chave de negócio).
     * UUID gerado pelo sistema.
     */
    private String idTransacao;

    /**
     * Status atual do pagamento.
     */
    private StatusPagamento status;

    /**
     * Valor da transação.
     */
    private BigDecimal valor;

    /**
     * Moeda da transação (ISO 4217).
     */
    private String moeda;

    /**
     * Data/hora da transação (ISO 8601 com timezone).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime dataHora;

    /**
     * Nome do estabelecimento.
     */
    private String estabelecimento;

    /**
     * Tipo de pagamento.
     */
    private TipoPagamento tipoPagamento;

    /**
     * Número de parcelas.
     */
    private Integer parcelas;

    /**
     * NSU - Número Sequencial Único (retornado após autorização).
     * Presente apenas se status = AUTORIZADO.
     */
    private String nsu;

    /**
     * Código de autorização (retornado pela adquirente).
     * Presente apenas se status = AUTORIZADO.
     */
    private String codigoAutorizacao;

    /**
     * Cartão mascarado (PCI-DSS).
     */
    private String cartaoMascarado;

    /**
     * Data/hora de criação do registro.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime criadoEm;

    /**
     * Data/hora da última atualização.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime atualizadoEm;

    /**
     * Mensagem adicional (ex: motivo de negação).
     * Presente apenas em casos especiais.
     */
    private String mensagem;
}
