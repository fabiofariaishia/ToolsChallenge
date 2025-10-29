package br.com.sicredi.toolschallenge.pagamento.domain;

/**
 * Enum que representa os tipos de pagamento disponíveis.
 * Sincronizado com o ENUM do banco: pagamento.tipo_pagamento
 */
public enum TipoPagamento {
    /**
     * Pagamento à vista (1 parcela)
     */
    AVISTA,
    
    /**
     * Parcelamento na loja (2 ou mais parcelas, juros absorvidos pelo estabelecimento)
     */
    PARCELADO_LOJA,
    
    /**
     * Parcelamento no emissor (2 ou mais parcelas, juros do emissor do cartão)
     */
    PARCELADO_EMISSOR
}
