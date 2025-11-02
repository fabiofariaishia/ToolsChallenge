package br.com.sicredi.toolschallenge.adquirente.service;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.adquirente.events.AutorizacaoRealizadaEvento;
import br.com.sicredi.toolschallenge.infra.outbox.publisher.EventoPublisher;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry meterRegistry;
    private EventoPublisher eventoPublisher;  // Removido 'final' para permitir @Autowired opcional

    public AdquirenteService(AdquirenteSimuladoService adquirenteSimulado, MeterRegistry meterRegistry) {
        this.adquirenteSimulado = adquirenteSimulado;
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Injeta EventoPublisher de forma opcional.
     * Pode ser null em ambientes de teste ou quando Kafka está desabilitado.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setEventoPublisher(EventoPublisher eventoPublisher) {
        this.eventoPublisher = eventoPublisher;
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
        
        AutorizacaoResponse response = adquirenteSimulado.autorizarPagamento(request);
        
        // Publicar evento de autorização realizada (sucesso)
        publicarEventoAutorizacao("PAGAMENTO", request, response, false, null);
        
        return response;
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
        AutorizacaoResponse response = new AutorizacaoResponse(StatusAutorizacao.PENDENTE, null, null);
        
        // Publicar evento de fallback
        publicarEventoAutorizacao("PAGAMENTO", request, response, true, ex.getMessage());
        
        return response;
    }
    
    /**
     * Processa estorno com mesma resiliência.
     */
    @CircuitBreaker(name = "adquirente", fallbackMethod = "processarEstornoFallback")
    @Retry(name = "adquirente")
    @Bulkhead(name = "adquirente", type = Bulkhead.Type.THREADPOOL)
    public AutorizacaoResponse processarEstorno(AutorizacaoRequest request) {
        log.info("Processando estorno com resiliência");
        
        AutorizacaoResponse response = adquirenteSimulado.processarEstorno(request);
        
        // Publicar evento de estorno realizado
        publicarEventoAutorizacao("ESTORNO", request, response, false, null);
        
        return response;
    }
    
    private AutorizacaoResponse processarEstornoFallback(
        AutorizacaoRequest request, 
        Exception ex
    ) {
        log.warn("🔴 FALLBACK ESTORNO - Marcando como PENDENTE");
        
        AutorizacaoResponse response = new AutorizacaoResponse(StatusAutorizacao.PENDENTE, null, null);
        
        // Publicar evento de fallback do estorno
        publicarEventoAutorizacao("ESTORNO", request, response, true, ex.getMessage());
        
        return response;
    }
    
    /**
     * Publica evento de autorização realizada no Outbox (para Kafka).
     * 
     * @param tipoOperacao PAGAMENTO ou ESTORNO
     * @param request Dados da requisição
     * @param response Resposta da autorização
     * @param fallbackAtivado Se foi ativado fallback (Circuit Breaker)
     * @param motivoFalha Mensagem de erro se houve falha
     */
    private void publicarEventoAutorizacao(
        String tipoOperacao,
        AutorizacaoRequest request,
        AutorizacaoResponse response,
        boolean fallbackAtivado,
        String motivoFalha
    ) {
        try {
            // Criar dados do evento
            AutorizacaoRealizadaEvento.DadosAutorizacao dados = new AutorizacaoRealizadaEvento.DadosAutorizacao(
                tipoOperacao,
                response.status(),
                request.valor(),
                maskCartao(request.numeroCartao()),
                response.nsu(),
                response.codigoAutorizacao(),
                fallbackAtivado,
                motivoFalha
            );
            
            // Criar evento
            String agregadoId = "autorizacao-" + System.currentTimeMillis();
            AutorizacaoRealizadaEvento evento = new AutorizacaoRealizadaEvento(
                agregadoId,
                dados
            );
            
            // Publicar evento apenas se EventoPublisher estiver disponível
            if (eventoPublisher == null) {
                log.warn("⚠️ EventoPublisher não disponível - evento de autorização não será publicado para Kafka");
            } else {
                // Publicar via método público do EventoPublisher
                eventoPublisher.publicarEventoGenerico(
                    agregadoId,
                    "Autorizacao",
                    "AUTORIZACAO_REALIZADA",
                    evento,
                    "adquirente.eventos"
                );
                
                log.debug("Evento de autorização publicado: tipo={}, status={}, fallback={}", 
                    tipoOperacao, response.status(), fallbackAtivado);
            }
                
        } catch (Exception ex) {
            // Não falhar a operação principal se publicação falhar
            log.error("Erro ao publicar evento de autorização: {}", ex.getMessage(), ex);
        }
    }
    
    private String maskCartao(String numeroCartao) {
        if (numeroCartao == null || numeroCartao.length() < 8) {
            return "****";
        }
        return numeroCartao.substring(0, 4) + "********" + numeroCartao.substring(numeroCartao.length() - 4);
    }
}
