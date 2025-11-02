package br.com.sicredi.toolschallenge.shared.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tratamento global de exceções para a API
 * Centraliza o mapeamento de exceções para respostas HTTP padronizadas
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Trata erros de validação de Bean Validation (@Valid)
     * Retorna 400 Bad Request com detalhes dos campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarErroValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Erro de validação na requisição: {}", traceId, request.getRequestURI());
        
        List<ErroResposta.CampoErro> errosValidacao = new ArrayList<>();
        
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            errosValidacao.add(new ErroResposta.CampoErro(
                    erro.getField(),
                    erro.getRejectedValue(),
                    erro.getDefaultMessage()
            ));
            logger.debug("[{}] Campo inválido: {} = {} ({})", 
                    traceId, erro.getField(), erro.getRejectedValue(), erro.getDefaultMessage());
        }
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados inválidos na requisição",
                request.getRequestURI()
        );
        resposta.setErrosValidacao(errosValidacao);
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata erros de validação de constraints (@NotNull, @Size, etc)
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResposta> tratarConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Violação de constraint na requisição: {}", traceId, request.getRequestURI());
        
        List<ErroResposta.CampoErro> errosValidacao = new ArrayList<>();
        
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String campo = violation.getPropertyPath().toString();
            errosValidacao.add(new ErroResposta.CampoErro(
                    campo,
                    violation.getInvalidValue(),
                    violation.getMessage()
            ));
            logger.debug("[{}] Constraint violada: {} = {} ({})", 
                    traceId, campo, violation.getInvalidValue(), violation.getMessage());
        }
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Violação de regras de validação",
                request.getRequestURI()
        );
        resposta.setErrosValidacao(errosValidacao);
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata erros de tipo de argumento inválido
     * Exemplo: passar string onde deveria ser número
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarErroTipoArgumento(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Erro de tipo de argumento: {} na requisição: {}", 
                traceId, ex.getName(), request.getRequestURI());
        
        Class<?> tipoRequerido = ex.getRequiredType();
        String nomeTipo = tipoRequerido != null ? tipoRequerido.getSimpleName() : "desconhecido";
        
        String mensagem = String.format(
                "Parâmetro '%s' com valor '%s' não pode ser convertido para o tipo '%s'",
                ex.getName(),
                ex.getValue(),
                nomeTipo
        );
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata exceções de negócio customizadas
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResposta> tratarErroNegocio(
            NegocioException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Erro de negócio: {} na requisição: {}", 
                traceId, ex.getMessage(), request.getRequestURI());
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata exceções de recurso não encontrado
     * Retorna 404 Not Found
     */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Recurso não encontrado: {} na requisição: {}", 
                traceId, ex.getMessage(), request.getRequestURI());
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resposta);
    }
    
    /**
     * Trata exceções de serviço indisponível (integrações externas)
     * Retorna 503 Service Unavailable
     * 
     * Usado quando:
     * - Adquirente de pagamentos está offline
     * - APIs externas não respondem (timeout)
     * - Circuit Breaker em estado OPEN
     * - Serviços de notificação indisponíveis
     */
    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarServicoIndisponivel(
            ServicoIndisponivelException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.error("[{}] Serviço indisponível: {} na requisição: {}", 
                traceId, ex.getMessage(), request.getRequestURI());
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(resposta);
    }
    
    /**
     * Trata IllegalArgumentException (argumentos inválidos)
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroResposta> tratarArgumentoIlegal(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Argumento ilegal: {} na requisição: {}", 
                traceId, ex.getMessage(), request.getRequestURI());
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata IllegalStateException (estado inválido)
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErroResposta> tratarEstadoIlegal(
            IllegalStateException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Estado ilegal: {} na requisição: {}", 
                traceId, ex.getMessage(), request.getRequestURI());
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata erros de deserialização JSON (incluindo enums inválidos)
     * Retorna 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarErroDesserializacao(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.warn("[{}] Erro de deserialização JSON na requisição: {}", 
                traceId, request.getRequestURI());
        
        String mensagem = "JSON mal formatado ou inválido";
        List<ErroResposta.CampoErro> errosValidacao = new ArrayList<>();
        
        // Tratamento especial para InvalidFormatException (enums inválidos)
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            
            // Se for erro de enum
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String fieldName = ife.getPath().get(0).getFieldName();
                String invalidValue = ife.getValue().toString();
                String validValues = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                
                mensagem = String.format(
                    "Valor '%s' inválido para o campo '%s'. Valores aceitos: %s",
                    invalidValue, fieldName, validValues
                );
                
                errosValidacao.add(new ErroResposta.CampoErro(
                    fieldName,
                    invalidValue,
                    String.format("Valor '%s' não é válido. Use: %s", invalidValue, validValues)
                ));
                
                logger.warn("[{}] Enum inválido: campo={}, valor={}, valores aceitos={}", 
                        traceId, fieldName, invalidValue, validValues);
            }
        }
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );
        if (!errosValidacao.isEmpty()) {
            resposta.setErrosValidacao(errosValidacao);
        }
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resposta);
    }
    
    /**
     * Trata todas as exceções não mapeadas
     * Retorna 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroGenerico(
            Exception ex,
            HttpServletRequest request) {
        
        String traceId = gerarTraceId();
        logger.error("[{}] Erro interno não tratado na requisição: {}", 
                traceId, request.getRequestURI(), ex);
        
        ErroResposta resposta = new ErroResposta(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocorreu um erro interno no servidor. Por favor, tente novamente mais tarde.",
                request.getRequestURI()
        );
        resposta.setTraceId(traceId);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
    }
    
    /**
     * Gera um ID único para rastreamento de erros
     */
    private String gerarTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
