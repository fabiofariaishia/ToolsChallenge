package br.com.sicredi.toolschallenge.infra.idempotencia.job;

import br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job agendado para limpeza de registros expirados de idempotência.
 * Executa a cada 1 hora para manter a tabela otimizada.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotenciaLimpezaJob {

    private final IdempotenciaService idempotenciaService;

    /**
     * Limpa registros expirados da tabela de idempotência.
     * Executa a cada 1 hora (3600000 ms).
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    public void limparRegistrosExpirados() {
        log.debug("Iniciando limpeza de registros expirados de idempotência");
        
        try {
            int removidos = idempotenciaService.limparRegistrosExpirados();
            
            if (removidos > 0) {
                log.info("Limpeza de idempotência concluída: {} registros removidos", removidos);
            } else {
                log.debug("Limpeza de idempotência concluída: nenhum registro expirado encontrado");
            }
            
        } catch (Exception e) {
            log.error("Erro ao executar limpeza de registros de idempotência", e);
        }
    }
}
