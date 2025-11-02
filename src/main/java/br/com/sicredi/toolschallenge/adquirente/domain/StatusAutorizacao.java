package br.com.sicredi.toolschallenge.adquirente.domain;

/**
 * Status de uma autorização junto ao adquirente.
 * 
 * Representa o resultado de uma tentativa de autorização de transação
 * (pagamento ou estorno) junto ao gateway de pagamento externo.
 */
public enum StatusAutorizacao {
    
    /**
     * Transação autorizada com sucesso pelo adquirente.
     * NSU e código de autorização foram gerados.
     */
    AUTORIZADO,
    
    /**
     * Transação negada pelo adquirente.
     * Possíveis motivos: saldo insuficiente, cartão bloqueado, limite excedido.
     */
    NEGADO,
    
    /**
     * Transação não pôde ser processada no momento.
     * 
     * Ocorre quando:
     * - Circuit Breaker está OPEN (adquirente indisponível)
     * - Timeout na comunicação
     * - Fallback ativado após esgotamento de retries
     * 
     * Transações PENDENTE podem ser reprocessadas posteriormente
     * quando o adquirente voltar a estar disponível.
     */
    PENDENTE
}
