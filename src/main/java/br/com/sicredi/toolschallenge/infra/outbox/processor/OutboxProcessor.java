package br.com.sicredi.toolschallenge.infra.outbox.processor;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import br.com.sicredi.toolschallenge.infra.outbox.service.KafkaPublisherService;
import br.com.sicredi.toolschallenge.infra.outbox.service.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Processador de eventos do Outbox Pattern.
 * 
 * Executa periodicamente (a cada 500ms) para:
 * 1. Buscar eventos PENDENTE no banco
 * 2. Publicar no Kafka
 * 3. Marcar como PROCESSADO ou ERRO
 * 
 * Implementa retry automático:
 * - Eventos com erro são reprocessados até 3 tentativas
 * - Após 3 tentativas, vão para Dead Letter Queue (DLQ)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    private final OutboxService outboxService;
    private final KafkaPublisherService kafkaPublisherService;

    // Processar eventos pendentes a cada 500ms
    private static final long INTERVALO_PROCESSAMENTO_MS = 500;
    
    // Processar eventos com erro a cada 5 segundos
    private static final long INTERVALO_RETRY_MS = 5000;
    
    // Quantidade máxima de eventos por batch
    private static final int BATCH_SIZE = 100;

    /**
     * Processa eventos pendentes.
     * Executa a cada 500ms.
     */
    @Scheduled(fixedRate = INTERVALO_PROCESSAMENTO_MS)
    public void processarEventosPendentes() {
        try {
            List<OutboxEvento> eventos = outboxService.buscarEventosPendentes(BATCH_SIZE);
            
            if (eventos.isEmpty()) {
                return; // Nada para processar
            }
            
            log.debug("Processando {} eventos pendentes do outbox", eventos.size());
            
            for (OutboxEvento evento : eventos) {
                processarEvento(evento);
            }
            
        } catch (Exception e) {
            log.error("Erro ao processar eventos pendentes do outbox", e);
        }
    }

    /**
     * Processa eventos com erro para retry.
     * Executa a cada 5 segundos.
     */
    @Scheduled(fixedRate = INTERVALO_RETRY_MS)
    public void processarEventosComErro() {
        try {
            List<OutboxEvento> eventos = outboxService.buscarEventosParaRetry();
            
            if (eventos.isEmpty()) {
                return; // Nada para reprocessar
            }
            
            log.info("Reprocessando {} eventos com erro do outbox", eventos.size());
            
            for (OutboxEvento evento : eventos) {
                processarEvento(evento);
            }
            
        } catch (Exception e) {
            log.error("Erro ao reprocessar eventos com erro do outbox", e);
        }
    }

    /**
     * Processa um único evento.
     * 
     * @param evento Evento a ser processado
     */
    private void processarEvento(OutboxEvento evento) {
        try {
            log.debug("Processando evento: id={}, tipo={}, agregado={}", 
                    evento.getId(), evento.getEventoTipo(), evento.getAgregadoId());
            
            // Publicar no Kafka
            kafkaPublisherService.publicarEvento(evento)
                    .thenAccept(result -> {
                        // Sucesso: marcar como processado
                        outboxService.marcarComoProcessado(evento.getId());
                    })
                    .exceptionally(ex -> {
                        // Erro: marcar como erro e incrementar tentativas
                        String mensagemErro = ex.getMessage();
                        if (mensagemErro == null) {
                            mensagemErro = ex.getClass().getSimpleName();
                        }
                        
                        // Limitar tamanho da mensagem de erro (max 1000 caracteres)
                        if (mensagemErro.length() > 1000) {
                            mensagemErro = mensagemErro.substring(0, 1000);
                        }
                        
                        outboxService.marcarComoErro(evento.getId(), mensagemErro);
                        
                        return null;
                    });
            
        } catch (Exception e) {
            log.error("Erro ao processar evento do outbox: id={}, tipo={}", 
                    evento.getId(), evento.getEventoTipo(), e);
            
            String mensagemErro = e.getMessage();
            if (mensagemErro == null) {
                mensagemErro = e.getClass().getSimpleName();
            }
            
            // Limitar tamanho da mensagem de erro
            if (mensagemErro.length() > 1000) {
                mensagemErro = mensagemErro.substring(0, 1000);
            }
            
            outboxService.marcarComoErro(evento.getId(), mensagemErro);
        }
    }

    /**
     * Limpa eventos processados antigos.
     * Executa diariamente à meia-noite.
     * Remove eventos processados há mais de 7 dias.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void limparEventosProcessadosAntigos() {
        try {
            log.info("Iniciando limpeza de eventos processados antigos");
            
            int diasRetencao = 7; // Manter eventos processados por 7 dias
            int removidos = outboxService.limparEventosProcessadosAntigos(diasRetencao);
            
            if (removidos > 0) {
                log.info("Limpeza de eventos concluída: {} eventos removidos", removidos);
            } else {
                log.debug("Limpeza de eventos concluída: nenhum evento removido");
            }
            
        } catch (Exception e) {
            log.error("Erro ao limpar eventos processados antigos", e);
        }
    }

    /**
     * Log de métricas do outbox.
     * Executa a cada 1 minuto.
     */
    @Scheduled(fixedRate = 60000)
    public void logMetricas() {
        try {
            Long pendentes = outboxService.contarEventosPendentes();
            Long comErro = outboxService.contarEventosComErro();
            
            if (pendentes > 0 || comErro > 0) {
                log.info("Métricas Outbox - Pendentes: {}, Com Erro: {}", pendentes, comErro);
            }
            
            // Alertar se muitos eventos com erro
            if (comErro > 10) {
                log.warn("ALERTA: {} eventos com erro no outbox! Verificar logs e DLQ.", comErro);
            }
            
        } catch (Exception e) {
            log.error("Erro ao coletar métricas do outbox", e);
        }
    }
}
