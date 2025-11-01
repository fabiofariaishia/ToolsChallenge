package br.com.sicredi.toolschallenge.infra.auditoria.scheduled;

import br.com.sicredi.toolschallenge.infra.auditoria.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Scheduler para tarefas periódicas de auditoria
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaScheduler {

    private final AuditoriaService auditoriaService;

    /**
     * Limpa eventos de auditoria antigos (executa diariamente à meia-noite)
     * Mantém eventos dos últimos 90 dias
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void limparEventosAntigos() {
        log.info("Iniciando limpeza de eventos de auditoria antigos");
        
        try {
            int diasRetencao = 90; // 90 dias de retenção
            auditoriaService.limparEventosAntigos(diasRetencao);
            log.info("Limpeza de eventos de auditoria concluída com sucesso");
        } catch (Exception e) {
            log.error("Erro ao limpar eventos de auditoria antigos: {}", e.getMessage(), e);
        }
    }

    /**
     * Gera métricas de auditoria (executa a cada 1 hora)
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    public void gerarMetricas() {
        try {
            Map<String, Long> stats = auditoriaService.obterEstatisticas();
            
            log.info("=== Métricas de Auditoria ===");
            log.info("Total de eventos: {}", stats.get("totalEventos"));
            log.info("Eventos de Pagamento: {}", stats.get("eventosPagamento"));
            log.info("Eventos de Estorno: {}", stats.get("eventosEstorno"));
            log.info("=============================");
            
        } catch (Exception e) {
            log.error("Erro ao gerar métricas de auditoria: {}", e.getMessage(), e);
        }
    }
}
