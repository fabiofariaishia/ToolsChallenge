package br.com.sicredi.toolschallenge.adquirente.service;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service que aplica resiliência na comunicação com o adquirente.
 * 
 * Padrões de resiliência aplicados:
 * 
 * 1. Circuit Breaker:
 *    - Abre após 50% de falhas em janela de 10 chamadas
 *    - Aguarda 10s antes de tentar half-open
 *    - Protege o sistema de sobrecarga quando adquirente está down
 * 
 * 2. Retry:
 *    - Até 3 tentativas com 500ms de intervalo
 *    - Só retenta em casos de falha temporária
 * 
 * 3. Bulkhead (Thread Pool):
 *    - Máximo 10 threads concorrentes
 *    - Isola recursos e previne esgotamento de threads
 * 
 * 4. Fallback:
 *    - Retorna resposta PENDENTE quando adquirente indisponível
 *    - Permite que API continue responsiva
 *    - Transação pode ser reprocessada posteriormente
 */
@Service
@Slf4j
public class AdquirenteService {

    private final AdquirenteSimuladoService adquirenteSimulado;

    public AdquirenteService(AdquirenteSimuladoService adquirenteSimulado) {
        this.adquirenteSimulado = adquirenteSimulado;
    }

    /**
     * Autoriza pagamento com resiliência completa.
     * 
     * Ordem de execução:
     * 1. Bulkhead verifica se há thread disponível
     * 2. Circuit Breaker verifica se está CLOSED
     * 3. Retry executa até 3 tentativas se falhar
     * 4. Fallback ativado se todas tentativas falharem
     * 
     * @param request Dados da transação
     * @return Resposta da autorização (ou PENDENTE se fallback)
     */
    @CircuitBreaker(name = "adquirente", fallbackMethod = "autorizarPagamentoFallback")
    @Retry(name = "adquirente")
    @Bulkhead(name = "adquirente", type = Bulkhead.Type.THREADPOOL)
    public AutorizacaoResponse autorizarPagamento(AutorizacaoRequest request) {
        log.info("Autorizando pagamento com resiliência: cartão={}", 
            maskCartao(request.numeroCartao()));
        
        return adquirenteSimulado.autorizarPagamento(request);
    }

    /**
     * Fallback executado quando:
     * - Circuit Breaker está OPEN
     * - Retry esgotou todas as tentativas
     * - Bulkhead está cheio (sem threads disponíveis)
     * 
     * Retorna resposta indicando PENDENTE para reprocessamento posterior.
     */
    private AutorizacaoResponse autorizarPagamentoFallback(
        AutorizacaoRequest request, 
        Exception ex
    ) {
        log.warn("🔴 FALLBACK ATIVADO - Adquirente indisponível. " +
                "Erro: {} - Marcando transação como PENDENTE", 
            ex.getClass().getSimpleName());
        
        // Retorna PENDENTE (sem NSU/código) para reprocessamento posterior
        return new AutorizacaoResponse(StatusAutorizacao.PENDENTE, null, null);
    }
    
    /**
     * Processa estorno com mesma resiliência.
     */
    @CircuitBreaker(name = "adquirente", fallbackMethod = "processarEstornoFallback")
    @Retry(name = "adquirente")
    @Bulkhead(name = "adquirente", type = Bulkhead.Type.THREADPOOL)
    public AutorizacaoResponse processarEstorno(AutorizacaoRequest request) {
        log.info("Processando estorno com resiliência");
        return adquirenteSimulado.processarEstorno(request);
    }
    
    private AutorizacaoResponse processarEstornoFallback(
        AutorizacaoRequest request, 
        Exception ex
    ) {
        log.warn("🔴 FALLBACK ESTORNO - Marcando como PENDENTE");
        return new AutorizacaoResponse(StatusAutorizacao.PENDENTE, null, null);
    }
    
    private String maskCartao(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() < 8) {
            return "****";
        }
        return numeroCartao.substring(0, 4) + "********" + numeroCartao.substring(numeroCartao.length() - 4);
    }
}
