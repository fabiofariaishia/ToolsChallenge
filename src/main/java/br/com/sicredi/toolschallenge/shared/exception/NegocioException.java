package br.com.sicredi.toolschallenge.shared.exception;

/**
 * Exception para erros de regra de negócio
 * Retorna HTTP 400 (Bad Request)
 */
public class NegocioException extends RuntimeException {
    
    public NegocioException(String mensagem) {
        super(mensagem);
    }
    
    public NegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
