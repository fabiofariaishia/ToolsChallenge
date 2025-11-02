package br.com.sicredi.toolschallenge.infra.outbox.publisher;

import br.com.sicredi.toolschallenge.infra.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publisher de eventos de domínio via Outbox Pattern.
 * 
 * Uso nos services:
 * <pre>
 * {@code
 * // Após salvar pagamento no banco (mesma transação)
 * eventoPublisher.publicarPagamentoCriado(pagamento);
 * }
 * </pre>
 * 
 * O evento é salvo na tabela outbox e será publicado no Kafka
 * pelo OutboxProcessor de forma assíncrona.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventoPublisher {

    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    // Tópicos Kafka
    private static final String TOPICO_PAGAMENTO = "pagamento.eventos";
    private static final String TOPICO_ESTORNO = "estorno.eventos";

    /**
     * Publica evento de pagamento criado.
     * 
     * @param evento Evento de domínio
     */
    public void publicarPagamentoCriado(Object evento) {
        publicarEvento(
                extrairId(evento, "idTransacao"),
                "Pagamento",
                "PagamentoCriado",
                evento,
                TOPICO_PAGAMENTO
        );
    }

    /**
     * Publica evento de status de pagamento alterado.
     * 
     * @param evento Evento de domínio
     */
    public void publicarPagamentoStatusAlterado(Object evento) {
        publicarEvento(
                extrairId(evento, "idTransacao"),
                "Pagamento",
                "PagamentoStatusAlterado",
                evento,
                TOPICO_PAGAMENTO
        );
    }

    /**
     * Publica evento de estorno criado.
     * 
     * @param evento Evento de domínio
     */
    public void publicarEstornoCriado(Object evento) {
        publicarEvento(
                extrairId(evento, "idTransacao"),
                "Estorno",
                "EstornoCriado",
                evento,
                TOPICO_ESTORNO
        );
    }

    /**
     * Publica evento de status de estorno alterado.
     * 
     * @param evento Evento de domínio
     */
    public void publicarEstornoStatusAlterado(Object evento) {
        publicarEvento(
                extrairId(evento, "idTransacao"),
                "Estorno",
                "EstornoStatusAlterado",
                evento,
                TOPICO_ESTORNO
        );
    }

    /**
     * Publica evento genérico de qualquer módulo.
     * 
     * Uso em módulos que não têm método específico (ex: adquirente):
     * <pre>
     * {@code
     * eventoPublisher.publicarEventoGenerico(
     *     "autorizacao-123", 
     *     "Autorizacao", 
     *     "AUTORIZACAO_REALIZADA", 
     *     evento, 
     *     "adquirente.eventos"
     * );
     * }
     * </pre>
     * 
     * @param agregadoId ID do agregado (ex: "autorizacao-123", "TXN-001")
     * @param agregadoTipo Tipo do agregado (ex: "Autorizacao", "Pagamento")
     * @param eventoTipo Tipo do evento (ex: "AUTORIZACAO_REALIZADA", "PagamentoCriado")
     * @param evento Objeto do evento (será convertido para Map automaticamente)
     * @param topicoKafka Tópico Kafka onde o evento será publicado (ex: "adquirente.eventos")
     */
    public void publicarEventoGenerico(
            String agregadoId,
            String agregadoTipo,
            String eventoTipo,
            Object evento,
            String topicoKafka) {
        
        publicarEvento(agregadoId, agregadoTipo, eventoTipo, evento, topicoKafka);
    }

    /**
     * Publica evento genérico no outbox.
     * 
     * @param agregadoId ID do agregado
     * @param agregadoTipo Tipo do agregado
     * @param eventoTipo Tipo do evento
     * @param evento Objeto do evento
     * @param topicoKafka Tópico Kafka
     */
    private void publicarEvento(
            String agregadoId,
            String agregadoTipo,
            String eventoTipo,
            Object evento,
            String topicoKafka) {
        
        try {
            // Converter evento para Map
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(evento, Map.class);
            
            // Salvar no outbox (mesma transação do agregado)
            outboxService.criarEvento(agregadoId, agregadoTipo, eventoTipo, payload, topicoKafka);
            
            log.debug("Evento publicado no outbox: tipo={}, agregado={}", eventoTipo, agregadoId);
            
        } catch (Exception e) {
            log.error("Erro ao publicar evento no outbox: tipo={}, agregado={}", 
                    eventoTipo, agregadoId, e);
            throw new RuntimeException("Erro ao publicar evento: " + e.getMessage(), e);
        }
    }

    /**
     * Extrai ID do agregado do evento.
     * 
     * @param evento Objeto do evento
     * @param campo Nome do campo (ex: "idTransacao")
     * @return ID do agregado
     */
    private String extrairId(Object evento, String campo) {
        try {
            Map<String, Object> map = objectMapper.convertValue(evento, Map.class);
            Object valor = map.get(campo);
            
            if (valor == null) {
                throw new IllegalArgumentException("Campo '" + campo + "' não encontrado no evento");
            }
            
            return String.valueOf(valor);
            
        } catch (Exception e) {
            log.error("Erro ao extrair campo '{}' do evento", campo, e);
            throw new RuntimeException("Erro ao extrair ID do evento: " + e.getMessage(), e);
        }
    }
}
