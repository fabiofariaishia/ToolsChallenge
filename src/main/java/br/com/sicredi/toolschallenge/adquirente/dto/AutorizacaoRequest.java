package br.com.sicredi.toolschallenge.adquirente.dto;

import java.math.BigDecimal;

/**
 * Request para autorização de pagamento/estorno com adquirente.
 * 
 * @param numeroCartao Número completo do cartão
 * @param cvv CVV do cartão
 * @param dataExpiracao Data de expiração (MM/YYYY)
 * @param valor Valor da transação
 * @param descricao Descrição da transação
 */
public record AutorizacaoRequest(
    String numeroCartao,
    String cvv,
    String dataExpiracao,
    BigDecimal valor,
    String descricao
) {}
