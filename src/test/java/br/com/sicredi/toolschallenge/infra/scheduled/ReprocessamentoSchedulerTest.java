package br.com.sicredi.toolschallenge.infra.scheduled;

import br.com.sicredi.toolschallenge.estorno.service.EstornoService;
import br.com.sicredi.toolschallenge.pagamento.service.PagamentoService;
import br.com.sicredi.toolschallenge.shared.config.ReprocessamentoProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link ReprocessamentoScheduler}.
 * 
 * <p>Valida o comportamento do scheduler de reprocessamento de transações pendentes,
 * garantindo que os métodos dos services são chamados corretamente e que exceções
 * são tratadas adequadamente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReprocessamentoScheduler - Testes Unitários")
class ReprocessamentoSchedulerTest {

    @Mock
    private EstornoService estornoService;

    @Mock
    private PagamentoService pagamentoService;

    @Mock
    private ReprocessamentoProperties properties;

    @InjectMocks
    private ReprocessamentoScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Configuração comum para todos os testes
    }

    // ========================================================================
    // Testes do Job de Estornos
    // ========================================================================

    @Test
    @DisplayName("Deve reprocessar estornos pendentes com sucesso")
    void deveReprocessarEstornosPendentesComSucesso() {
        // Arrange
        doNothing().when(estornoService).reprocessarEstornosPendentes();

        // Act
        scheduler.reprocessarEstornosPendentes();

        // Assert
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
    }

    @Test
    @DisplayName("Deve capturar exceção ao reprocessar estornos e não propagar erro")
    void deveCapturaExcecaoAoReprocessarEstornos() {
        // Arrange
        RuntimeException exception = new RuntimeException("Erro simulado no serviço de estornos");
        doThrow(exception).when(estornoService).reprocessarEstornosPendentes();

        // Act - Não deve lançar exceção
        scheduler.reprocessarEstornosPendentes();

        // Assert
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
        // O scheduler logou o erro mas não propagou a exceção
    }

    @Test
    @DisplayName("Deve executar job de estornos mesmo quando não há registros pendentes")
    void deveExecutarJobEstornosQuandoNaoHaRegistrosPendentes() {
        // Arrange - Service retorna sem fazer nada (nenhum registro encontrado)
        doNothing().when(estornoService).reprocessarEstornosPendentes();

        // Act
        scheduler.reprocessarEstornosPendentes();

        // Assert
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
        // Job executou normalmente mesmo sem registros para processar
    }

    // ========================================================================
    // Testes do Job de Pagamentos
    // ========================================================================

    @Test
    @DisplayName("Deve reprocessar pagamentos pendentes com sucesso")
    void deveReprocessarPagamentosPendentesComSucesso() {
        // Arrange
        doNothing().when(pagamentoService).reprocessarPagamentosPendentes();

        // Act
        scheduler.reprocessarPagamentosPendentes();

        // Assert
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
    }

    @Test
    @DisplayName("Deve capturar exceção ao reprocessar pagamentos e não propagar erro")
    void deveCapturaExcecaoAoReprocessarPagamentos() {
        // Arrange
        RuntimeException exception = new RuntimeException("Erro simulado no serviço de pagamentos");
        doThrow(exception).when(pagamentoService).reprocessarPagamentosPendentes();

        // Act - Não deve lançar exceção
        scheduler.reprocessarPagamentosPendentes();

        // Assert
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
        // O scheduler logou o erro mas não propagou a exceção
    }

    @Test
    @DisplayName("Deve executar job de pagamentos mesmo quando não há registros pendentes")
    void deveExecutarJobPagamentosQuandoNaoHaRegistrosPendentes() {
        // Arrange - Service retorna sem fazer nada (nenhum registro encontrado)
        doNothing().when(pagamentoService).reprocessarPagamentosPendentes();

        // Act
        scheduler.reprocessarPagamentosPendentes();

        // Assert
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
        // Job executou normalmente mesmo sem registros para processar
    }

    // ========================================================================
    // Testes de Independência entre Jobs
    // ========================================================================

    @Test
    @DisplayName("Deve executar jobs de estornos e pagamentos independentemente")
    void deveExecutarJobsIndependentemente() {
        // Arrange
        doNothing().when(estornoService).reprocessarEstornosPendentes();
        doNothing().when(pagamentoService).reprocessarPagamentosPendentes();

        // Act - Executa ambos os jobs
        scheduler.reprocessarEstornosPendentes();
        scheduler.reprocessarPagamentosPendentes();

        // Assert - Ambos foram executados
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
    }

    @Test
    @DisplayName("Erro no job de estornos não deve afetar job de pagamentos")
    void erroNoJobEstornosNaoDeveAfetarJobPagamentos() {
        // Arrange
        RuntimeException exception = new RuntimeException("Erro no job de estornos");
        doThrow(exception).when(estornoService).reprocessarEstornosPendentes();
        doNothing().when(pagamentoService).reprocessarPagamentosPendentes();

        // Act
        scheduler.reprocessarEstornosPendentes();  // Falha mas não propaga exceção
        scheduler.reprocessarPagamentosPendentes(); // Executa normalmente

        // Assert
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
    }

    @Test
    @DisplayName("Erro no job de pagamentos não deve afetar job de estornos")
    void erroNoJobPagamentosNaoDeveAfetarJobEstornos() {
        // Arrange
        RuntimeException exception = new RuntimeException("Erro no job de pagamentos");
        doNothing().when(estornoService).reprocessarEstornosPendentes();
        doThrow(exception).when(pagamentoService).reprocessarPagamentosPendentes();

        // Act
        scheduler.reprocessarEstornosPendentes();   // Executa normalmente
        scheduler.reprocessarPagamentosPendentes(); // Falha mas não propaga exceção

        // Assert
        verify(estornoService, times(1)).reprocessarEstornosPendentes();
        verify(pagamentoService, times(1)).reprocessarPagamentosPendentes();
    }

    // ========================================================================
    // Testes de Múltiplas Execuções
    // ========================================================================

    @Test
    @DisplayName("Deve permitir múltiplas execuções consecutivas do job de estornos")
    void devePermitirMultiplasExecucoesJobEstornos() {
        // Arrange
        doNothing().when(estornoService).reprocessarEstornosPendentes();

        // Act - Simula 3 execuções do scheduler (a cada 5 minutos)
        scheduler.reprocessarEstornosPendentes();
        scheduler.reprocessarEstornosPendentes();
        scheduler.reprocessarEstornosPendentes();

        // Assert
        verify(estornoService, times(3)).reprocessarEstornosPendentes();
    }

    @Test
    @DisplayName("Deve permitir múltiplas execuções consecutivas do job de pagamentos")
    void devePermitirMultiplasExecucoesJobPagamentos() {
        // Arrange
        doNothing().when(pagamentoService).reprocessarPagamentosPendentes();

        // Act - Simula 3 execuções do scheduler (a cada 5 minutos)
        scheduler.reprocessarPagamentosPendentes();
        scheduler.reprocessarPagamentosPendentes();
        scheduler.reprocessarPagamentosPendentes();

        // Assert
        verify(pagamentoService, times(3)).reprocessarPagamentosPendentes();
    }
}
