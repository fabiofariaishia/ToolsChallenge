package br.com.sicredi.toolschallenge.adquirente.events;

import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de domínio publicado quando uma autorização é realizada junto ao adquirente.
 * 
 * Publicado em:
 * - Autorização de pagamento (sucesso ou falha)
 * - Processamento de estorno (sucesso ou falha)
 * - Fallback ativado (Circuit Breaker OPEN ou timeout)
 * 
 * Consumido por:
 * - Sistema de auditoria (registro completo de autorizações)
 * - Sistema de métricas (taxa de aprovação, falhas)
 * - Sistema de alertas (Circuit Breaker aberto, alta taxa de falha)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutorizacaoRealizadaEvento {
    
    private String tipoEvento = "AUTORIZACAO_REALIZADA";
    private LocalDateTime timestamp = LocalDateTime.now();
    private String agregadoId; // ID da transação original (pagamento ou estorno)
    private DadosAutorizacao dados;
    
    public AutorizacaoRealizadaEvento(String agregadoId, DadosAutorizacao dados) {
        this.agregadoId = agregadoId;
        this.dados = dados;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * Dados completos da autorização realizada
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DadosAutorizacao {
        private String tipoOperacao; // "PAGAMENTO" ou "ESTORNO"
        private StatusAutorizacao status; // AUTORIZADO, NEGADO, PENDENTE
        private BigDecimal valor;
        private String cartaoMascarado;
        private String nsu;
        private String codigoAutorizacao;
        private Boolean fallbackAtivado; // true se Circuit Breaker ativou fallback
        private String motivoFalha; // Exception message se houve falha
    }
}
