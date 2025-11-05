package br.com.sicredi.toolschallenge.pagamento.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enum que representa os tipos de pagamento disponíveis.
 * Sincronizado com o ENUM do banco: pagamento.tipo_pagamento
 */
@Schema(description = "Tipos de pagamento disponíveis")
public enum TipoPagamento {
    /**
     * Pagamento à vista (1 parcela)
     */
    @Schema(description = "Pagamento à vista (1 parcela)")
    AVISTA,
    
    /**
     * Parcelamento na loja (2 ou mais parcelas, juros absorvidos pelo estabelecimento)
     */
    @Schema(description = "Parcelamento na loja (2 ou mais parcelas, juros absorvidos pelo estabelecimento)")
    PARCELADO_LOJA,
    
    /**
     * Parcelamento no emissor (2 ou mais parcelas, juros do emissor do cartão)
     */
    @Schema(description = "Parcelamento no emissor (2 ou mais parcelas, juros do emissor do cartão)")
    PARCELADO_EMISSOR
}
