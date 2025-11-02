package br.com.sicredi.toolschallenge.infra.scheduled;

import br.com.sicredi.toolschallenge.estorno.service.EstornoService;
import br.com.sicredi.toolschallenge.pagamento.service.PagamentoService;
import br.com.sicredi.toolschallenge.shared.config.ReprocessamentoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler para reprocessamento de transações pendentes.
 * 
 * <p>Reprocessa automaticamente transações que ficaram no status PENDENTE devido a:
 * <ul>
 *   <li>Circuit Breaker aberto (adquirente indisponível)</li>
 *   <li>Falhas temporárias de rede</li>
 *   <li>Timeouts na comunicação com adquirente</li>
 * </ul>
 * 
 * <p>Executa a cada 5 minutos (padrão), tentando reprocessar transações pendentes até o limite
 * máximo de tentativas configurado (padrão: 3 tentativas).
 * 
 * <p>Após atingir o limite de tentativas, a transação é enviada para análise manual (DLQ).
 * 
 * <p><b>Configuração:</b>
 * <pre>
 * reprocessamento:
 *   enabled: true              # Habilita/desabilita o scheduler
 *   intervalo-minutos: 5       # Intervalo entre execuções
 *   batch-size: 50             # Máximo de registros por batch
 *   max-tentativas: 3          # Tentativas antes de DLQ
 * </pre>
 * 
 * <p><b>Desabilitar em testes:</b>
 * <pre>
 * # application-test.yml
 * reprocessamento:
 *   enabled: false
 * </pre>
 * 
 * @see EstornoService#reprocessarEstornosPendentes()
 * @see PagamentoService#reprocessarPagamentosPendentes()
 * @see ReprocessamentoProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reprocessamento.enabled", havingValue = "true", matchIfMissing = true)
public class ReprocessamentoScheduler {

    private final EstornoService estornoService;
    private final PagamentoService pagamentoService;
    private final ReprocessamentoProperties properties;

    /**
     * Reprocessa estornos pendentes a cada 5 minutos.
     * 
     * <p>Busca todos os estornos com status PENDENTE e tenta reprocessá-los com o adquirente.
     * Atualiza o status baseado na resposta (CANCELADO, NEGADO ou mantém PENDENTE).
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void reprocessarEstornosPendentes() {
        long startTime = System.currentTimeMillis();
        log.info("📅 [SCHEDULER] Iniciando job de reprocessamento de estornos pendentes");
        
        try {
            estornoService.reprocessarEstornosPendentes();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [SCHEDULER] Job de reprocessamento de estornos concluído com sucesso (tempo: {}ms)", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [SCHEDULER] Erro ao reprocessar estornos pendentes (tempo: {}ms): {}", duration, e.getMessage(), e);
        }
    }

    /**
     * Reprocessa pagamentos pendentes a cada 5 minutos.
     * 
     * <p>Busca todos os pagamentos com status PENDENTE e tenta autorizá-los novamente com o adquirente.
     * Atualiza o status baseado na resposta (PROCESSADO, ERRO ou mantém PENDENTE).
     */
    @Scheduled(fixedRate = 300000) // 5 minutos
    public void reprocessarPagamentosPendentes() {
        long startTime = System.currentTimeMillis();
        log.info("📅 [SCHEDULER] Iniciando job de reprocessamento de pagamentos pendentes");
        
        try {
            pagamentoService.reprocessarPagamentosPendentes();
            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ [SCHEDULER] Job de reprocessamento de pagamentos concluído com sucesso (tempo: {}ms)", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ [SCHEDULER] Erro ao reprocessar pagamentos pendentes (tempo: {}ms): {}", duration, e.getMessage(), e);
        }
    }
}
