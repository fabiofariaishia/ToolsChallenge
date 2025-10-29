package br.com.sicredi.toolschallenge.estorno.domain;

/**
 * Enum que representa os possíveis status de um estorno.
 * Sincronizado com o ENUM do banco: estorno.status_estorno
 */
public enum StatusEstorno {
    /**
     * Estorno solicitado, aguardando processamento
     */
    PENDENTE,
    
    /**
     * Estorno concluído com sucesso (pagamento cancelado)
     */
    CANCELADO,
    
    /**
     * Estorno negado (fora da janela de 24h, pagamento já cancelado, etc.)
     */
    NEGADO
}
