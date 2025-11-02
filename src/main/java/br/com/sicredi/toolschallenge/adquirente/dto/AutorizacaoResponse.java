package br.com.sicredi.toolschallenge.adquirente.dto;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;

/**
 * Response da autorização de pagamento/estorno do adquirente.
 * 
 * @param status Status da autorização (AUTORIZADO, NEGADO, PENDENTE)
 * @param nsu Número Sequencial Único (gerado pelo adquirente quando AUTORIZADO)
 * @param codigoAutorizacao Código de autorização (gerado pelo adquirente quando AUTORIZADO)
 */
public record AutorizacaoResponse(
    StatusAutorizacao status,
    String nsu,
    String codigoAutorizacao
) {
    /**
     * Verifica se a transação foi autorizada.
     */
    public boolean autorizado() {
        return status == StatusAutorizacao.AUTORIZADO;
    }
    
    /**
     * Verifica se é uma resposta pendente (fallback ativado).
     * Ocorre quando Circuit Breaker está OPEN ou após esgotamento de retries.
     */
    public boolean isPendente() {
        return status == StatusAutorizacao.PENDENTE;
    }
    
    /**
     * Verifica se a transação foi negada pelo adquirente.
     */
    public boolean negado() {
        return status == StatusAutorizacao.NEGADO;
    }
}
