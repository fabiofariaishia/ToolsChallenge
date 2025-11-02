package br.com.sicredi.toolschallenge.shared.exception;

/**
 * Exception genérica para indicar que um serviço externo está indisponível.
 * 
 * Retorna HTTP 503 (Service Unavailable).
 * 
 * Usada para:
 * - Acionar Circuit Breaker e Retry (Resilience4j)
 * - Indicar falhas temporárias em integrações externas
 * - Permitir que a API permaneça responsiva durante indisponibilidades
 * 
 * Exemplos de uso:
 * - Adquirente de pagamentos offline
 * - API de consulta de CEP indisponível
 * - Serviço de notificação (SMS/Email) fora do ar
 * - Gateway de pagamento com timeout
 */
public class ServicoIndisponivelException extends RuntimeException {
    
    public ServicoIndisponivelException(String mensagem) {
        super(mensagem);
    }
    
    public ServicoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
