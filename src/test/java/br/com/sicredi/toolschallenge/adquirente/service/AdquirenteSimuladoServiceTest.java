package br.com.sicredi.toolschallenge.adquirente.service;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.shared.exception.ServicoIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AdquirenteSimuladoService - Testes Unitários")
class AdquirenteSimuladoServiceTest {

    @Test
    @DisplayName("Deve autorizar pagamento com sucesso quando taxa de aprovação é 100%")
    void deveAutorizarPagamentoComSucessoQuandoTaxaAprovacao100() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);  // 100% aprovação
        
        AutorizacaoRequest request = criarRequest();

        // Act
        AutorizacaoResponse response = service.autorizarPagamento(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusAutorizacao.AUTORIZADO);
        assertThat(response.nsu()).isNotNull();
        assertThat(response.codigoAutorizacao()).isNotNull();
    }

    @Test
    @DisplayName("Deve negar pagamento quando taxa de aprovação é 0%")
    void deveNegarPagamentoQuandoTaxaAprovacao0() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 0.0);  // 0% aprovação
        
        AutorizacaoRequest request = criarRequest();

        // Act
        AutorizacaoResponse response = service.autorizarPagamento(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusAutorizacao.NEGADO);
        assertThat(response.nsu()).isNull();
        assertThat(response.codigoAutorizacao()).isNull();
    }

    @Test
    @DisplayName("Deve lançar exception quando taxa de falha é 100%")
    void deveLancarExceptionQuandoTaxaFalha100() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 1.0);  // 100% falha
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act & Assert
        assertThatThrownBy(() -> service.autorizarPagamento(request))
            .isInstanceOf(ServicoIndisponivelException.class)
            .hasMessageContaining("Adquirente temporariamente indisponível");
    }

    @Test
    @DisplayName("Deve gerar NSU com formato correto")
    void deveGerarNSUComFormatoCorreto() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act
        AutorizacaoResponse response = service.autorizarPagamento(request);

        // Assert
        assertThat(response.nsu())
            .isNotNull()
            .matches("\\d{10}");  // Regex: 10 dígitos
    }

    @Test
    @DisplayName("Deve gerar código de autorização com formato correto")
    void deveGerarCodigoAutorizacaoComFormatoCorreto() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act
        AutorizacaoResponse response = service.autorizarPagamento(request);

        // Assert
        assertThat(response.codigoAutorizacao())
            .isNotNull()
            .matches("\\d{6}");  // Regex: 6 dígitos
    }

    @Test
    @DisplayName("Deve processar estorno com sucesso")
    void deveProcessarEstornoComSucesso() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act
        AutorizacaoResponse response = service.processarEstorno(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(StatusAutorizacao.AUTORIZADO);
    }

    @Test
    @DisplayName("Deve aplicar latência quando configurada (teste rápido com 10ms)")
    void deveAplicarLatenciaQuandoConfigurada() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 10);  // 10ms é aceitável para teste
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act
        long inicio = System.currentTimeMillis();
        service.autorizarPagamento(request);
        long duracao = System.currentTimeMillis() - inicio;

        // Assert - deve levar pelo menos 10ms
        assertThat(duracao).isGreaterThanOrEqualTo(10L);
    }

    @Test
    @DisplayName("Deve funcionar sem latência quando latency-ms é zero")
    void deveFuncionarSemLatenciaQuandoZero() {
        // Arrange
        AdquirenteSimuladoService service = new AdquirenteSimuladoService();
        ReflectionTestUtils.setField(service, "failureRate", 0.0);
        ReflectionTestUtils.setField(service, "latencyMs", 0);  // Zero latência
        ReflectionTestUtils.setField(service, "timeoutRate", 0.0);
        ReflectionTestUtils.setField(service, "aprovacaoRate", 1.0);
        
        AutorizacaoRequest request = criarRequest();

        // Act
        long inicio = System.currentTimeMillis();
        AutorizacaoResponse response = service.autorizarPagamento(request);
        long duracao = System.currentTimeMillis() - inicio;

        // Assert
        assertThat(response).isNotNull();
        assertThat(duracao).isLessThan(100L);  // Deve ser rápido (< 100ms)
    }

    // Método auxiliar para criar request de teste
    private AutorizacaoRequest criarRequest() {
        return new AutorizacaoRequest(
            "4111111111111111",
            "123",
            "12/2025",
            BigDecimal.valueOf(100.50),
            "Compra de teste"
        );
    }
}
