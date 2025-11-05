package br.com.sicredi.toolschallenge.estorno.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

/**
 * DTO para resposta de estorno.
 * 
 * Retorna APENAS o objeto transacao com dados da transação estornada.
 * Campos nulos são omitidos do JSON.
 * 
 * Estrutura conforme imagem de referência:
 * {
 *   "transacao": {
 *     "cartao": "4444********1234",
 *     "id": "10002356890001",
 *     "descricao": {
 *       "valor": "50.00",
 *       "dataHora": "01/05/2021 18:30:00",
 *       "estabelecimento": "PetShop Mundo cão",
 *       "nsu": "1234567890",
 *       "codigoAutorizacao": "147258369",
 *       "status": "CANCELADO"
 *     },
 *     "formaPagamento": {
 *       "tipo": "AVISTA",
 *       "parcelas": "1"
 *     }
 *   }
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta com dados da transação estornada")
public class EstornoResponseDTO {

    /**
     * Dados da transação estornada (estrutura aninhada).
     * Contém descricao (com nsu, codigoAutorizacao, status do ESTORNO) + formaPagamento.
     */
    @Schema(
        description = "Dados completos da transação estornada",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Valid
    private TransacaoEstornoDTO transacao;
}
