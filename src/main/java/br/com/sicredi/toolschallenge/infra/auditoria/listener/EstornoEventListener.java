package br.com.sicredi.toolschallenge.infra.auditoria.listener;

import br.com.sicredi.toolschallenge.estorno.events.EstornoCriadoEvento;
import br.com.sicredi.toolschallenge.estorno.events.EstornoStatusAlteradoEvento;
import br.com.sicredi.toolschallenge.infra.auditoria.service.AuditoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Listener para eventos de domínio de Estorno.
 * Registra eventos na auditoria para rastreabilidade completa.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstornoEventListener {

    private final AuditoriaService auditoriaService;
    private final ObjectMapper objectMapper;

    /**
     * Escuta eventos de criação de estorno
     */
    @EventListener
    public void onEstornoCriado(EstornoCriadoEvento evento) {
        log.debug("Recebido evento de estorno criado: {}", evento.getIdEstorno());
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(evento, Map.class);
            
            // Adiciona metadados sobre o pagamento relacionado
            Map<String, Object> metadados = new HashMap<>();
            metadados.put("idPagamentoOriginal", evento.getIdPagamento().toString());
            metadados.put("valorEstorno", evento.getValorEstorno());
            metadados.put("temMotivo", evento.getMotivo() != null && !evento.getMotivo().isEmpty());
            
            auditoriaService.registrarEventoComMetadados(
                    "ESTORNO",
                    evento.getIdEstorno().toString(),
                    "ESTORNO_CRIADO",
                    payload,
                    metadados
            );
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria de estorno criado: {}", e.getMessage(), e);
        }
    }

    /**
     * Escuta eventos de alteração de status de estorno
     */
    @EventListener
    public void onEstornoStatusAlterado(EstornoStatusAlteradoEvento evento) {
        log.debug("Recebido evento de status de estorno alterado: {} -> {}", 
                evento.getStatusAnterior(), evento.getStatusNovo());
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.convertValue(evento, Map.class);
            
            // Adiciona metadados sobre a mudança de status
            Map<String, Object> metadados = new HashMap<>();
            metadados.put("statusAnterior", evento.getStatusAnterior());
            metadados.put("statusNovo", evento.getStatusNovo());
            metadados.put("idPagamento", evento.getIdPagamento().toString());
            metadados.put("temMotivo", evento.getMotivo() != null && !evento.getMotivo().isEmpty());
            
            auditoriaService.registrarEventoComMetadados(
                    "ESTORNO",
                    evento.getIdEstorno().toString(),
                    "ESTORNO_STATUS_ALTERADO",
                    payload,
                    metadados
            );
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria de status de estorno alterado: {}", e.getMessage(), e);
        }
    }
}
