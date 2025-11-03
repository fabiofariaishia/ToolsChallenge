package br.com.sicredi.toolschallenge.pagamento.service;

import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.adquirente.service.AdquirenteService;
import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.shared.config.ReprocessamentoProperties;
import br.com.sicredi.toolschallenge.infra.outbox.publisher.EventoPublisher;
import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.repository.PagamentoRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para PagamentoService - Foco em Reprocessamento (Item 8).
 * 
 * Cobertura:
 * - Reprocessamento com sucesso (PENDENTE → AUTORIZADO)
 * - Reprocessamento com falha (PENDENTE → PENDENTE, incrementa tentativas)
 * - Reprocessamento com NEGADO (PENDENTE → NEGADO)
 * - Reprocessamento com max tentativas atingidas (DLQ)
 * - Batch vazio (retorna sem erro)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoService - Testes Unitários de Reprocessamento")
class PagamentoServiceTest {

    @Mock
    private PagamentoRepository repository;

    @Mock
    private AdquirenteService adquirenteService;

    @Mock
    private EventoPublisher eventoPublisher;

    @Mock
    private ReprocessamentoProperties reprocessamentoProperties;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter dlqCounter;

    @InjectMocks
    private PagamentoService pagamentoService;

    private AutorizacaoResponse autorizacaoAprovada;
    private AutorizacaoResponse autorizacaoNegada;
    private AutorizacaoResponse autorizacaoPendente;

    @BeforeEach
    void setUp() {
        // Configurar maxTentativas = 3 (padrão do projeto)
        when(reprocessamentoProperties.getMaxTentativas()).thenReturn(3);

        // Respostas do adquirente
        autorizacaoAprovada = new AutorizacaoResponse(
            StatusAutorizacao.AUTORIZADO,
            "NSU123456",
            "AUTH001"
        );

        autorizacaoNegada = new AutorizacaoResponse(
            StatusAutorizacao.NEGADO,
            null,
            null
        );

        autorizacaoPendente = new AutorizacaoResponse(
            StatusAutorizacao.PENDENTE,
            null,
            null
        );
    }

    // ==================== TESTES DE REPROCESSAMENTO (ITEM 8) ====================

    @Test
    @DisplayName("1. Deve reprocessar pagamento PENDENTE com sucesso → AUTORIZADO")
    void deveReprocessarPagamentoPendenteComSucesso() {
        // Arrange
        Pagamento pagamentoPendente = new Pagamento();
        pagamentoPendente.setId(1L);
        pagamentoPendente.setIdTransacao("TXN-PAG-001");
        pagamentoPendente.setStatus(StatusPagamento.PENDENTE);
        pagamentoPendente.setTentativasReprocessamento(0);
        pagamentoPendente.setValor(new BigDecimal("100.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pagamentoPendente));
        when(adquirenteService.autorizarPagamento(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada);
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService).autorizarPagamento(any(AutorizacaoRequest.class));
        verify(repository).save(argThat(p -> 
            p.getStatus() == StatusPagamento.AUTORIZADO &&
            p.getTentativasReprocessamento() == 1 &&
            p.getNsu().equals("NSU123456") &&
            p.getCodigoAutorizacao().equals("AUTH001")
        ));
        verify(eventoPublisher).publicarPagamentoStatusAlterado(any());
    }

    @Test
    @DisplayName("2. Deve reprocessar pagamento PENDENTE com falha → PENDENTE (incrementa tentativas)")
    void deveReprocessarPagamentoPendenteComFalha() {
        // Arrange
        Pagamento pagamentoPendente = new Pagamento();
        pagamentoPendente.setId(2L);
        pagamentoPendente.setIdTransacao("TXN-PAG-002");
        pagamentoPendente.setStatus(StatusPagamento.PENDENTE);
        pagamentoPendente.setTentativasReprocessamento(1); // Já tentou 1x
        pagamentoPendente.setValor(new BigDecimal("200.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pagamentoPendente));
        when(adquirenteService.autorizarPagamento(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoPendente); // Ainda pendente (Circuit Breaker OPEN)
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService).autorizarPagamento(any(AutorizacaoRequest.class));
        verify(repository).save(argThat(p -> 
            p.getStatus() == StatusPagamento.PENDENTE &&
            p.getTentativasReprocessamento() == 2 // Incrementou de 1 para 2
        ));
        verify(eventoPublisher, never()).publicarPagamentoStatusAlterado(any()); // Status não mudou
    }

    @Test
    @DisplayName("3. Deve reprocessar pagamento PENDENTE → NEGADO")
    void deveReprocessarPagamentoPendenteParaNegado() {
        // Arrange
        Pagamento pagamentoPendente = new Pagamento();
        pagamentoPendente.setId(3L);
        pagamentoPendente.setIdTransacao("TXN-PAG-003");
        pagamentoPendente.setStatus(StatusPagamento.PENDENTE);
        pagamentoPendente.setTentativasReprocessamento(0);
        pagamentoPendente.setValor(new BigDecimal("50.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pagamentoPendente));
        when(adquirenteService.autorizarPagamento(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoNegada); // Adquirente negou
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService).autorizarPagamento(any(AutorizacaoRequest.class));
        verify(repository).save(argThat(p -> 
            p.getStatus() == StatusPagamento.NEGADO &&
            p.getTentativasReprocessamento() == 1
        ));
        verify(eventoPublisher).publicarPagamentoStatusAlterado(any());
    }

    @Test
    @DisplayName("4. Não deve reprocessar pagamento com max tentativas atingidas (DLQ)")
    void naoDeveReprocessarPagamentoComMaxTentativas() {
        // Arrange
        Pagamento pagamentoDLQ = new Pagamento();
        pagamentoDLQ.setId(4L);
        pagamentoDLQ.setIdTransacao("TXN-PAG-DLQ");
        pagamentoDLQ.setStatus(StatusPagamento.PENDENTE);
        pagamentoDLQ.setTentativasReprocessamento(3); // MAX = 3
        pagamentoDLQ.setValor(new BigDecimal("75.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pagamentoDLQ));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService, never()).autorizarPagamento(any()); // NÃO tentou reprocessar
        verify(repository, never()).save(any()); // NÃO salvou
        verify(eventoPublisher, never()).publicarPagamentoStatusAlterado(any());
    }

    @Test
    @DisplayName("5. Deve retornar sem erro quando batch está vazio")
    void deveRetornarSemErroQuandoBatchVazio() {
        // Arrange
        when(repository.findPagamentosPendentes())
            .thenReturn(Collections.emptyList()); // Lista vazia

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService, never()).autorizarPagamento(any());
        verify(repository, never()).save(any());
        verify(eventoPublisher, never()).publicarPagamentoStatusAlterado(any());
    }

    @Test
    @DisplayName("6. Deve reprocessar múltiplos pagamentos em um batch")
    void deveReprocessarMultiplosPagamentos() {
        // Arrange
        Pagamento pag1 = new Pagamento();
        pag1.setId(1L);
        pag1.setIdTransacao("TXN-BATCH-1");
        pag1.setStatus(StatusPagamento.PENDENTE);
        pag1.setTentativasReprocessamento(0);
        pag1.setValor(new BigDecimal("100.00"));

        Pagamento pag2 = new Pagamento();
        pag2.setId(2L);
        pag2.setIdTransacao("TXN-BATCH-2");
        pag2.setStatus(StatusPagamento.PENDENTE);
        pag2.setTentativasReprocessamento(1);
        pag2.setValor(new BigDecimal("200.00"));

        Pagamento pag3 = new Pagamento();
        pag3.setId(3L);
        pag3.setIdTransacao("TXN-BATCH-3");
        pag3.setStatus(StatusPagamento.PENDENTE);
        pag3.setTentativasReprocessamento(3); // DLQ
        pag3.setValor(new BigDecimal("300.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pag1, pag2, pag3));
        when(adquirenteService.autorizarPagamento(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada); // Todos que forem reprocessados terão sucesso
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService, times(2)).autorizarPagamento(any()); // 2x (pag1 e pag2, pag3 foi para DLQ)
        verify(repository, times(2)).save(any()); // 2x salvamentos
        verify(eventoPublisher, times(2)).publicarPagamentoStatusAlterado(any()); // 2x eventos
    }

    @Test
    @DisplayName("7. Deve capturar e logar exceções sem interromper batch")
    void deveCapturaExcecoesSemInterromperBatch() {
        // Arrange
        Pagamento pag1 = new Pagamento();
        pag1.setId(1L);
        pag1.setIdTransacao("TXN-ERR-1");
        pag1.setStatus(StatusPagamento.PENDENTE);
        pag1.setTentativasReprocessamento(0);
        pag1.setValor(new BigDecimal("100.00"));

        Pagamento pag2 = new Pagamento();
        pag2.setId(2L);
        pag2.setIdTransacao("TXN-OK-2");
        pag2.setStatus(StatusPagamento.PENDENTE);
        pag2.setTentativasReprocessamento(0);
        pag2.setValor(new BigDecimal("200.00"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pag1, pag2));
        
        // Primeiro pagamento lança exceção, segundo funciona
        when(adquirenteService.autorizarPagamento(any(AutorizacaoRequest.class)))
            .thenThrow(new RuntimeException("Erro simulado"))
            .thenReturn(autorizacaoAprovada);
        
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert - Deve processar os 2 pagamentos (1 com erro, 1 com sucesso)
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService, times(2)).autorizarPagamento(any());
        verify(repository, times(1)).save(argThat(p -> 
            p.getIdTransacao().equals("TXN-OK-2") && p.getStatus() == StatusPagamento.AUTORIZADO
        ));
        verify(eventoPublisher, times(1)).publicarPagamentoStatusAlterado(any());
    }

    @Test
    @DisplayName("8. Deve incrementar métrica DLQ quando pagamento atinge máximo de tentativas")
    void deveIncrementarMetricaDLQQuandoPagamentoAtingeMaximoTentativas() {
        // Arrange - Configurar mock do counter DLQ (apenas neste teste)
        when(meterRegistry.counter("reprocessamento.dlq.total", "tipo", "pagamento"))
            .thenReturn(dlqCounter);
        
        // Arrange - Pagamento que já atingiu max tentativas
        Pagamento pagamentoDLQ = new Pagamento();
        pagamentoDLQ.setId(99L);
        pagamentoDLQ.setIdTransacao("TXN-DLQ-METRICS");
        pagamentoDLQ.setStatus(StatusPagamento.PENDENTE);
        pagamentoDLQ.setTentativasReprocessamento(3); // MAX = 3
        pagamentoDLQ.setValor(new BigDecimal("999.99"));

        when(repository.findPagamentosPendentes())
            .thenReturn(Arrays.asList(pagamentoDLQ));

        // Act
        pagamentoService.reprocessarPagamentosPendentes();

        // Assert - Verificar que counter DLQ foi incrementado
        verify(meterRegistry).counter("reprocessamento.dlq.total", "tipo", "pagamento");
        verify(dlqCounter).increment(); // Counter foi incrementado
        verify(repository).findPagamentosPendentes();
        verify(adquirenteService, never()).autorizarPagamento(any()); // Não processou
        verify(repository, never()).save(any()); // Não salvou
    }
}
