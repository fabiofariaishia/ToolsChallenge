package br.com.sicredi.toolschallenge.pagamento.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os possíveis status de um pagamento.
 * Sincronizado com o ENUM do banco: pagamento.status_pagamento
 */
@Schema(description = "Status possíveis de um pagamento")
public enum StatusPagamento {
    /**
     * Pagamento criado, aguardando processamento/autorização
     */
    @Schema(description = "Pagamento criado, aguardando processamento/autorização")
    PENDENTE,
    
    /**
     * Pagamento autorizado pelo adquirente
     */
    @Schema(description = "Pagamento autorizado pelo adquirente")
    AUTORIZADO,
    
    /**
     * Pagamento negado pelo adquirente (saldo insuficiente, cartão bloqueado, etc.)
     */
    @Schema(description = "Pagamento negado pelo adquirente (saldo insuficiente, cartão bloqueado, etc.)")
    NEGADO
}
