package br.com.sicredi.toolschallenge.infra.idempotencia.interceptor;

import br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Map;
import java.util.UUID;

/**
 * Advice para capturar e armazenar respostas de requisições idempotentes.
 * Executa APÓS o controller retornar a resposta, mas ANTES de serializar.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class IdempotenciaResponseAdvice implements ResponseBodyAdvice<Object> {

    private final IdempotenciaService idempotenciaService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(@NonNull MethodParameter returnType, 
                           @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        // Suportar todos os métodos (filtraremos depois)
        return true;
    }

    @Override
    @Nullable
    public Object beforeBodyWrite(@Nullable Object body, 
                                   @NonNull MethodParameter returnType, 
                                   @NonNull MediaType selectedContentType,
                                   @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   @NonNull ServerHttpRequest request, 
                                   @NonNull ServerHttpResponse response) {
        
        if (!(request instanceof ServletServerHttpRequest servletRequest) ||
            !(response instanceof ServletServerHttpResponse servletResponse)) {
            return body;
        }

        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        
        // Verificar se é uma requisição idempotente
        String chave = (String) httpRequest.getAttribute(IdempotenciaInterceptor.CHAVE_IDEMPOTENCIA_ATTR);
        if (chave == null) {
            return body;
        }

        // Só salvar respostas 2xx
        int statusCode = servletResponse.getServletResponse().getStatus();
        if (statusCode < 200 || statusCode >= 300) {
            log.debug("Resposta com status {} não será armazenada para chave: {}", statusCode, chave);
            return body;
        }

        try {
            // Converter body para Map
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = body instanceof Map ? 
                (Map<String, Object>) body : 
                objectMapper.convertValue(body, Map.class);

            // Extrair TTL
            Long ttl = (Long) httpRequest.getAttribute(IdempotenciaInterceptor.TTL_ATTR);
            if (ttl == null) {
                ttl = 86400L; // 24 horas
            }

            // Extrair ID da transação
            String idTransacao = extrairIdTransacao(responseMap);

            // Armazenar
            String endpoint = httpRequest.getMethod() + " " + httpRequest.getRequestURI();
            idempotenciaService.salvarResposta(chave, idTransacao, endpoint, responseMap, statusCode, ttl);
            
            log.info("Resposta salva para chave de idempotência: {}", chave);

        } catch (Exception e) {
            log.error("Erro ao armazenar resposta de idempotência para chave: {}", chave, e);
        }

        return body;
    }

    private String extrairIdTransacao(Map<String, Object> responseMap) {
        Object id = responseMap.getOrDefault("id", 
                    responseMap.getOrDefault("idTransacao",
                    responseMap.getOrDefault("idPagamento",
                    responseMap.get("idEstorno"))));
        
        return id != null ? String.valueOf(id) : UUID.randomUUID().toString();
    }
}
