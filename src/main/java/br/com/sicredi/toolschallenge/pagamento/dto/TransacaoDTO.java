package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * DTO principal representando uma transação completa.
 * 
 * Estrutura aninhada:
 * - cartao: número do cartão mascarado
 * - id: identificador único da transação
 * - descricao: dados da transação (valor, dataHora, estabelecimento)
 * - formaPagamento: tipo e parcelas
 * - nsu: Número Sequencial Único (apenas na response)
 * - codigoAutorizacao: código de autorização (apenas na response)
 * - status: status da transação (apenas na response)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "cartao")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Dados completos da transação de pagamento")
public class TransacaoDTO {

    /**
     * Número do cartão mascarado (PCI-DSS compliant).
     * Formato esperado: 4 primeiros + * + 4 últimos dígitos
     * Exemplo: "4444********1234"
     */
    @Schema(
        description = "Número do cartão mascarado para segurança PCI-DSS (primeiros 4 + ******** + últimos 4 dígitos)",
        example = "4444********1234",
        pattern = "^\\d{4}\\*{8}\\d{4}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Número do cartão mascarado é obrigatório")
    @Pattern(
        regexp = "^\\d{4}\\*{8}\\d{4}$",
        message = "Cartão mascarado deve estar no formato: 4444********1234"
    )
    private String cartao;

    /**
     * ID único da transação.
     * Formato: número de 13 dígitos
     * Exemplo: "10002356890001"
     */
    @Schema(
        description = "Identificador único da transação (13 dígitos)",
        example = "10002356890001",
        pattern = "^\\d{13}$"
    )
    @Pattern(
        regexp = "^\\d{13}$",
        message = "ID da transação deve conter exatamente 13 dígitos"
    )
    private String id;

    /**
     * Descrição detalhada da transação.
     */
    @Schema(
        description = "Descrição detalhada da transação",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Descrição da transação é obrigatória")
    @Valid
    private DescricaoDTO descricao;

    /**
     * Forma de pagamento (tipo e parcelas).
     */
    @Schema(
        description = "Forma de pagamento",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Forma de pagamento é obrigatória")
    @Valid
    private FormaPagamentoDTO formaPagamento;

    /**
     * NSU - Número Sequencial Único (retornado após autorização).
     * Presente apenas na response se status = AUTORIZADO.
     */
    @Schema(
        description = "NSU - Número Sequencial Único da transação (presente apenas se autorizado)",
        example = "1234567890"
    )
    private String nsu;

    /**
     * Código de autorização (retornado pela adquirente).
     * Presente apenas na response se status = AUTORIZADO.
     */
    @Schema(
        description = "Código de autorização da adquirente (presente apenas se autorizado)",
        example = "147258369"
    )
    private String codigoAutorizacao;

    /**
     * Status atual da transação.
     * Presente apenas na response.
     */
    @Schema(
        description = "Status atual do pagamento",
        example = "AUTORIZADO",
        allowableValues = {"PENDENTE", "AUTORIZADO", "NEGADO"}
    )
    private StatusPagamento status;
}
