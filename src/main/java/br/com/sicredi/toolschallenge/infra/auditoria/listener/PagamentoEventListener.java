package br.com.sicredi.toolschallenge.infra.auditoria.listener;

import br.com.sicredi.toolschallenge.infra.auditoria.service.AuditoriaService;
import br.com.sicredi.toolschallenge.pagamento.events.PagamentoCriadoEvento;
import br.com.sicredi.toolschallenge.pagamento.events.PagamentoStatusAlteradoEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener para eventos de domínio de Pagamento.
 * Registra eventos na auditoria para rastreabilidade completa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PagamentoEventListener {

    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    /**
     * Escuta eventos de criação de pagamento
     */
    @EventListener
    public void onPagamentoCriado(PagamentoCriadoEvento evento) {
        log.debug("Recebido evento de pagamento criado: {}", evento.getIdPagamento());
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(evento, Map.class);
            
            auditoriaService.registrarEvento(
                    "PAGAMENTO",
                    evento.getIdPagamento().toString(),
                    "PAGAMENTO_CRIADO",
                    payload
            );
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria de pagamento criado: {}", e.getMessage(), e);
        }
    }

    /**
     * Escuta eventos de alteração de status de pagamento
     */
    @EventListener
    public void onPagamentoStatusAlterado(PagamentoStatusAlteradoEvento evento) {
        log.debug("Recebido evento de status alterado: {} -> {}", 
                evento.getStatusAnterior(), evento.getStatusNovo());
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(evento, Map.class);
            
            // Adiciona metadados sobre a mudança de status
            Map<String, Object> metadados = new HashMap<>();
            metadados.put("statusAnterior", evento.getStatusAnterior());
            metadados.put("statusNovo", evento.getStatusNovo());
            metadados.put("temMotivo", evento.getMotivo() != null && !evento.getMotivo().isEmpty());
            
            auditoriaService.registrarEventoComMetadados(
                    "PAGAMENTO",
                    evento.getIdPagamento().toString(),
                    "PAGAMENTO_STATUS_ALTERADO",
                    payload,
                    metadados
            );
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria de status alterado: {}", e.getMessage(), e);
        }
    }
}
