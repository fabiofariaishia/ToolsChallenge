package br.com.sicredi.toolschallenge.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propriedades de configuração para reprocessamento de transações pendentes.
 * 
 * <p>Mapeia as configurações do application.yml na seção 'reprocessamento'.
 * 
 * <p>Exemplo de uso:
 * <pre>
 * reprocessamento:
 *   enabled: true
 *   intervalo-minutos: 5
 *   batch-size: 50
 *   max-tentativas: 3
 * </pre>
 * 
 * @see br.com.sicredi.toolschallenge.infra.scheduled.ReprocessamentoScheduler
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "reprocessamento")
public class ReprocessamentoProperties {

    /**
     * Habilita ou desabilita o scheduler de reprocessamento.
     * 
     * <p>Útil para desabilitar em ambientes de teste ou desenvolvimento.
     * 
     * <p>Padrão: true
     */
    private boolean enabled = true;

    /**
     * Intervalo entre execuções do scheduler em minutos.
     * 
     * <p>Padrão: 5 minutos
     */
    private int intervaloMinutos = 5;

    /**
     * Número máximo de registros processados por batch.
     * 
     * <p>Limita a carga de processamento em cada execução do scheduler.
     * 
     * <p>Padrão: 50 registros
     */
    private int batchSize = 50;

    /**
     * Número máximo de tentativas de reprocessamento antes de enviar para DLQ.
     * 
     * <p>Após atingir esse limite, a transação é marcada para análise manual.
     * 
     * <p>Padrão: 3 tentativas
     */
    private int maxTentativas = 3;
}
