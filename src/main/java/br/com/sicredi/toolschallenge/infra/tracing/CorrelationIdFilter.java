package br.com.sicredi.toolschallenge.infra.tracing;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtro HTTP que gerencia Correlation ID para rastreamento de requisições.
 * 
 * Fluxo:
 * 1. Extrai X-Correlation-ID do header (se presente)
 * 2. Se ausente, gera novo UUID
 * 3. Adiciona ao MDC (Mapped Diagnostic Context) para logs
 * 4. Propaga para response header
 * 5. Remove do MDC após processamento (cleanup)
 * 
 * Integração com Micrometer Tracing:
 * - MDC automaticamente recebe traceId e spanId do Micrometer
 * - X-Correlation-ID é um ID de negócio adicional controlado pelo cliente
 * 
 * Ordem de execução: HIGHEST_PRECEDENCE para executar antes de outros filtros
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    /**
     * Nome do header HTTP para correlation ID
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    /**
     * Nome da chave no MDC (sincronizada com Logback pattern)
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpRequest && response instanceof HttpServletResponse httpResponse) {
            
            // 1. Extrair ou gerar correlation ID
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Correlation ID gerado: {}", correlationId);
            } else {
                log.debug("Correlation ID recebido: {}", correlationId);
            }
            
            try {
                // 2. Adicionar ao MDC (disponível para todos os logs)
                MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
                
                // 3. Adicionar ao response header (para cliente rastrear)
                httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
                
                // 4. Continuar processamento
                chain.doFilter(request, response);
                
            } finally {
                // 5. Cleanup: Remover do MDC após processamento
                MDC.remove(CORRELATION_ID_MDC_KEY);
            }
            
        } else {
            // Requisição não-HTTP (improvável em Spring Boot)
            chain.doFilter(request, response);
        }
    }
}
