package br.com.sicredi.toolschallenge.pagamento.domain;

/**
 * Enum que representa os possíveis status de um pagamento.
 * Sincronizado com o ENUM do banco: pagamento.status_pagamento
 */
public enum StatusPagamento {
    /**
     * Pagamento criado, aguardando processamento/autorização
     */
    PENDENTE,
    
    /**
     * Pagamento autorizado pelo adquirente
     */
    AUTORIZADO,
    
    /**
     * Pagamento negado pelo adquirente (saldo insuficiente, cartão bloqueado, etc.)
     */
    NEGADO
}
