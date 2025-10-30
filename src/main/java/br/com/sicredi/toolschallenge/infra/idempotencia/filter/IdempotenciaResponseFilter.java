package br.com.sicredi.toolschallenge.infra.idempotencia.filter;

import br.com.sicredi.toolschallenge.infra.idempotencia.interceptor.IdempotenciaInterceptor;
import br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Filtro para capturar e armazenar respostas de requisições idempotentes.
 * 
 * Executa APÓS o processamento do controller.
 * Só armazena respostas bem-sucedidas (2xx, 3xx).
 * 
 * Funciona em conjunto com {@link IdempotenciaInterceptor}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotenciaResponseFilter extends OncePerRequestFilter {

    private final IdempotenciaService idempotenciaService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Verificar se é uma requisição idempotente
        String chave = (String) request.getAttribute(IdempotenciaInterceptor.CHAVE_IDEMPOTENCIA_ATTR);
        
        if (chave == null) {
            // Não é uma requisição idempotente, continuar normalmente
            filterChain.doFilter(request, response);
            return;
        }

        // Wrappear response para capturar corpo
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            // Processar requisição
            filterChain.doFilter(request, responseWrapper);

            // Capturar resposta
            int statusCode = responseWrapper.getStatus();
            HttpStatus httpStatus = HttpStatus.valueOf(statusCode);

            // Só armazenar respostas bem-sucedidas (2xx, 3xx)
            if (httpStatus.is2xxSuccessful() || httpStatus.is3xxRedirection()) {
                armazenarResposta(request, responseWrapper, chave, statusCode);
            } else {
                log.debug("Resposta com status {} não será armazenada para chave: {}", statusCode, chave);
            }

        } finally {
            // IMPORTANTE: Copiar conteúdo para a resposta real
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Armazena resposta no cache de idempotência.
     */
    private void armazenarResposta(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper, 
                                     String chave, int statusCode) {
        try {
            // Extrair corpo da resposta
            byte[] responseBody = responseWrapper.getContentAsByteArray();
            String responseBodyStr = new String(responseBody, StandardCharsets.UTF_8);

            // Converter para Map
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = objectMapper.readValue(responseBodyStr, Map.class);

            // Extrair TTL
            Long ttl = (Long) request.getAttribute(IdempotenciaInterceptor.TTL_ATTR);
            if (ttl == null) {
                ttl = 86400L; // TTL padrão: 24 horas
            }

            // Extrair ou gerar ID da transação
            String idTransacao = extrairIdTransacao(responseMap);

            // Armazenar
            String endpoint = request.getMethod() + " " + request.getRequestURI();
            idempotenciaService.salvarResposta(chave, idTransacao, endpoint, responseMap, statusCode, ttl);
            
            log.info("Resposta salva para chave de idempotência: {}", chave);

        } catch (Exception e) {
            log.error("Erro ao armazenar resposta de idempotência para chave: {}", chave, e);
        }
    }

    /**
     * Extrai ID da transação do corpo da resposta.
     * Tenta campos comuns: id, idPagamento, idEstorno.
     * Se não encontrar, gera UUID.
     */
    private String extrairIdTransacao(Map<String, Object> responseMap) {
        Object id = responseMap.get("id");
        if (id != null) {
            return String.valueOf(id);
        }

        id = responseMap.get("idPagamento");
        if (id != null) {
            return String.valueOf(id);
        }

        id = responseMap.get("idEstorno");
        if (id != null) {
            return String.valueOf(id);
        }

        // Gerar UUID se não encontrou ID
        return UUID.randomUUID().toString();
    }
}
