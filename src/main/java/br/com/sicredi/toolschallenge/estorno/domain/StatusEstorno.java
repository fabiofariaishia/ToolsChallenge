package br.com.sicredi.toolschallenge.estorno.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os possíveis status de um estorno.
 * Sincronizado com o ENUM do banco: estorno.status_estorno
 */
@Schema(description = "Status possíveis de um estorno")
public enum StatusEstorno {
    /**
     * Estorno solicitado, aguardando processamento
     */
    @Schema(description = "Estorno solicitado, aguardando processamento")
    PENDENTE,
    
    /**
     * Estorno concluído com sucesso (pagamento cancelado)
     */
    @Schema(description = "Estorno concluído com sucesso (pagamento cancelado)")
    CANCELADO,
    
    /**
     * Estorno negado (fora da janela de 24h, pagamento já cancelado, etc.)
     */
    @Schema(description = "Estorno negado (fora da janela de 24h, pagamento já cancelado, etc.)")
    NEGADO
}
