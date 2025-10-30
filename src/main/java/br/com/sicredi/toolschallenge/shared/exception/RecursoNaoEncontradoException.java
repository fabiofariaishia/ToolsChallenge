package br.com.sicredi.toolschallenge.shared.exception;

/**
 * Exception para recursos não encontrados
 * Retorna HTTP 404 (Not Found)
 */
public class RecursoNaoEncontradoException extends RuntimeException {
    
    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
    
    public RecursoNaoEncontradoException(String tipoRecurso, String identificador) {
        super(String.format("%s não encontrado(a) com identificador: %s", tipoRecurso, identificador));
    }
}
