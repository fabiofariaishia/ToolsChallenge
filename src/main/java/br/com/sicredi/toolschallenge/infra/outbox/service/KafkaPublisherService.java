package br.com.sicredi.toolschallenge.infra.outbox.service;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service para publicar eventos no Kafka.
 * 
 * Funciona em conjunto com OutboxProcessor:
 * 1. OutboxProcessor busca eventos pendentes
 * 2. KafkaPublisherService publica no Kafka
 * 3. Se sucesso: OutboxService marca como PROCESSADO
 * 4. Se erro: OutboxService marca como ERRO e incrementa tentativas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publica evento do outbox no Kafka.
     * 
     * @param evento Evento do outbox
     * @return CompletableFuture com resultado do envio
     */
    public CompletableFuture<SendResult<String, String>> publicarEvento(OutboxEvento evento) {
        try {
            // Construir mensagem Kafka
            Map<String, Object> mensagem = construirMensagem(evento);
            String mensagemJson = objectMapper.writeValueAsString(mensagem);
            
            // Chave para particionamento (usa agregadoId para manter ordem)
            String chave = evento.getAgregadoId();
            
            log.debug("Publicando evento no Kafka: topico={}, chave={}, tipo={}", 
                    evento.getTopicoKafka(), chave, evento.getEventoTipo());
            
            // Publicar no Kafka
            return kafkaTemplate.send(evento.getTopicoKafka(), chave, mensagemJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Evento publicado com sucesso no Kafka: id={}, tipo={}, topico={}, partition={}, offset={}", 
                                    evento.getId(), 
                                    evento.getEventoTipo(), 
                                    evento.getTopicoKafka(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Erro ao publicar evento no Kafka: id={}, tipo={}, erro={}", 
                                    evento.getId(), evento.getEventoTipo(), ex.getMessage(), ex);
                        }
                    });
            
        } catch (Exception e) {
            log.error("Erro ao serializar evento para Kafka: id={}, tipo={}", 
                    evento.getId(), evento.getEventoTipo(), e);
            
            CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * Constrói mensagem Kafka com metadados.
     * 
     * Estrutura da mensagem:
     * {
     *   "eventoId": 123,
     *   "eventoTipo": "PagamentoAutorizado",
     *   "agregadoId": "PAG-001",
     *   "agregadoTipo": "Pagamento",
     *   "timestamp": "2025-10-30T19:30:00Z",
     *   "payload": { ... dados do evento ... }
     * }
     */
    private Map<String, Object> construirMensagem(OutboxEvento evento) {
        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("eventoId", evento.getId());
        mensagem.put("eventoTipo", evento.getEventoTipo());
        mensagem.put("agregadoId", evento.getAgregadoId());
        mensagem.put("agregadoTipo", evento.getAgregadoTipo());
        mensagem.put("timestamp", evento.getCriadoEm().toString());
        mensagem.put("payload", evento.getPayload());
        
        return mensagem;
    }

    /**
     * Publica evento de teste (para desenvolvimento/debug).
     * 
     * @param topico Tópico Kafka
     * @param chave Chave da mensagem
     * @param mensagem Conteúdo da mensagem
     */
    public void publicarEventoTeste(String topico, String chave, Map<String, Object> mensagem) {
        try {
            String mensagemJson = objectMapper.writeValueAsString(mensagem);
            
            kafkaTemplate.send(topico, chave, mensagemJson)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Evento de teste publicado: topico={}, chave={}, partition={}, offset={}", 
                                    topico, chave, 
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Erro ao publicar evento de teste: topico={}, chave={}", 
                                    topico, chave, ex);
                        }
                    });
            
        } catch (Exception e) {
            log.error("Erro ao serializar evento de teste: topico={}, chave={}", topico, chave, e);
        }
    }
}
