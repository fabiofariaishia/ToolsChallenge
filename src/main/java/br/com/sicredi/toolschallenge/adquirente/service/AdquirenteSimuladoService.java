package br.com.sicredi.toolschallenge.adquirente.service;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.shared.exception.ServicoIndisponivelException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Simula um adquirente externo (gateway de pagamento).
 * 
 * Permite configurar taxa de falha e latência via properties para testar resiliência.
 * 
 * Configurações:
 * - adquirente.simulado.failure-rate: Taxa de falhas (0.0 a 1.0)
 * - adquirente.simulado.latency-ms: Latência em milissegundos
 * - adquirente.simulado.timeout-rate: Taxa de timeouts (0.0 a 1.0)
 */
@Service
@Slf4j
public class AdquirenteSimuladoService {

    @Value("${adquirente.simulado.failure-rate:0.0}")
    private double failureRate;
    
    @Value("${adquirente.simulado.latency-ms:100}")
    private int latencyMs;
    
    @Value("${adquirente.simulado.timeout-rate:0.0}")
    private double timeoutRate;
    
    @Value("${adquirente.simulado.aprovacao-rate:0.9}")
    private double aprovacaoRate;
    
    private final Random random = new Random();

    /**
     * Simula autorização de pagamento com adquirente externo.
     * 
     * Comportamento:
     * 1. Aplica latência configurada
     * 2. Pode simular timeout (latência extrema)
     * 3. Pode lançar exception (adquirente indisponível)
     * 4. Retorna autorizado/negado baseado em taxa de aprovação
     * 
     * @param request Dados da transação
     * @return Resposta da autorização
     * @throws ServicoIndisponivelException Se simular falha
     */
    public AutorizacaoResponse autorizarPagamento(AutorizacaoRequest request) {
        log.debug("Simulando autorização - latência: {}ms, falha: {}%, timeout: {}%, aprovação: {}%",
            latencyMs, failureRate * 100, timeoutRate * 100, aprovacaoRate * 100);
        
        // 1. Simular latência base
        simularLatencia(latencyMs);
        
        // 2. Simular timeout (latência extrema)
        if (random.nextDouble() < timeoutRate) {
            log.warn("Simulando TIMEOUT do adquirente (30 segundos)");
            simularLatencia(30000);
        }
        
        // 3. Simular falha do adquirente
        if (random.nextDouble() < failureRate) {
            log.error("Simulando FALHA do adquirente - Lançando exception");
            throw new ServicoIndisponivelException("Adquirente temporariamente indisponível");
        }
        
        // 4. Sucesso - Gerar resposta
        boolean autorizado = random.nextDouble() < aprovacaoRate;
        
        if (autorizado) {
            String nsu = gerarNSU();
            String codigo = gerarCodigoAutorizacao();
            log.info("Autorização APROVADA - NSU: {}, Código: {}", nsu, codigo);
            return new AutorizacaoResponse(StatusAutorizacao.AUTORIZADO, nsu, codigo);
        } else {
            log.info("Autorização NEGADA pelo adquirente");
            return new AutorizacaoResponse(StatusAutorizacao.NEGADO, null, null);
        }
    }
    
    /**
     * Simula processamento de estorno (mesma lógica de autorização).
     */
    public AutorizacaoResponse processarEstorno(AutorizacaoRequest request) {
        log.debug("Simulando processamento de estorno");
        return autorizarPagamento(request);
    }
    
    private void simularLatencia(int ms) {
        if (ms <= 0) return;
        
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulação de latência interrompida");
        }
    }
    
    private String gerarNSU() {
        return String.format("%010d", random.nextInt(1_000_000_000));
    }
    
    private String gerarCodigoAutorizacao() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
