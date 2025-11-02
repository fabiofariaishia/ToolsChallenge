package br.com.sicredi.toolschallenge.infra.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link CorrelationIdFilter}
 */
@ExtendWith(MockitoExtension.class)
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear(); // Limpar MDC antes de cada teste
    }

    @AfterEach
    void tearDown() {
        MDC.clear(); // Limpar MDC após cada teste
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderAusente() throws Exception {
        // Arrange
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(null);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void deveUsarCorrelationIdDoHeaderQuandoPresente() throws Exception {
        // Arrange
        String correlationId = "test-correlation-123";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(response).setHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        verify(chain).doFilter(request, response);
    }

    @Test
    void deveAdicionarCorrelationIdAoMDCDuranteProcessamento() throws Exception {
        // Arrange
        String correlationId = "test-correlation-456";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);

        // Act
        doAnswer(invocation -> {
            // Durante o processamento, MDC deve conter correlation ID
            String mdcValue = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
            assertEquals(correlationId, mdcValue, "MDC deve conter correlation ID durante processamento");
            return null;
        }).when(chain).doFilter(request, response);

        filter.doFilter(request, response, chain);

        // Assert
        verify(chain).doFilter(request, response);
    }

    @Test
    void deveRemoverCorrelationIdDoMDCApósProcessamento() throws Exception {
        // Arrange
        String correlationId = "test-correlation-789";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        String mdcValueAfter = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        assertNull(mdcValueAfter, "MDC deve estar limpo após processamento");
    }

    @Test
    void deveRemoverMDCMesmoQuandoOcorreExcecao() throws Exception {
        // Arrange
        String correlationId = "test-correlation-error";
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn(correlationId);
        doThrow(new RuntimeException("Erro simulado")).when(chain).doFilter(request, response);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> filter.doFilter(request, response, chain));

        // MDC deve estar limpo mesmo após exceção
        String mdcValueAfter = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        assertNull(mdcValueAfter, "MDC deve estar limpo após exceção");
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderVazio() throws Exception {
        // Arrange
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("");

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString());
        verify(chain).doFilter(request, response);
    }

    @Test
    void deveGerarCorrelationIdQuandoHeaderApenasBranco() throws Exception {
        // Arrange
        when(request.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).thenReturn("   ");

        // Act
        filter.doFilter(request, response, chain);

        // Assert
        verify(response).setHeader(eq(CorrelationIdFilter.CORRELATION_ID_HEADER), anyString());
        verify(chain).doFilter(request, response);
    }
}
