package br.com.sicredi.toolschallenge.estorno.dto;

import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class EstornoResponseDTO {

    /**
     * ID da transação original do pagamento.
     */
    private String idTransacao;

    /**
     * ID único do estorno (gerado pelo sistema).
     */
    private String idEstorno;

    /**
     * Status atual do estorno.
     */
    private StatusEstorno status;

    /**
     * Valor estornado.
     */
    private BigDecimal valor;

    /**
     * Data/hora da solicitação do estorno (ISO 8601 com timezone).
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime dataHora;

    /**
     * NSU do estorno (retornado após cancelamento).
     * Presente apenas se status = CANCELADO.
     */
    private String nsu;

    /**
     * Código de autorização do estorno.
     * Presente apenas se status = CANCELADO.
     */
    private String codigoAutorizacao;

    /**
     * Motivo do estorno fornecido pelo solicitante.
     */
    private String motivo;

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
     */
    private String mensagem;
}
