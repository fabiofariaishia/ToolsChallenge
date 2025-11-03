package br.com.sicredi.toolschallenge.shared.config;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de métricas Micrometer.
 * 
 * Habilita suporte para annotations @Timed e @Counted via AOP.
 * 
 * Spring Boot 3.x não habilita automaticamente esses aspects,
 * então precisamos configurá-los manualmente.
 */
@Configuration
public class MetricsConfig {

    /**
     * Habilita @Timed annotation via AOP.
     * 
     * Permite que métodos anotados com @Timed(value="metric.name")
     * registrem automaticamente timers (latência) no MeterRegistry.
     * 
     * Exemplos de uso:
     * - @Timed(value = "pagamento.criacao.tempo", histogram = true)
     * - @Timed(value = "estorno.criacao.tempo", histogram = true)
     * 
     * @param registry MeterRegistry do Spring Boot (auto-injetado)
     * @return TimedAspect configurado
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Habilita @Counted annotation via AOP.
     * 
     * Permite que métodos anotados com @Counted(value="metric.name")
     * registrem automaticamente counters (contadores) no MeterRegistry.
     * 
     * Exemplos de uso:
     * - @Counted(value = "pagamento.criados")
     * - @Counted(value = "estorno.criados")
     * 
     * @param registry MeterRegistry do Spring Boot (auto-injetado)
     * @return CountedAspect configurado
     */
    @Bean
    public CountedAspect countedAspect(MeterRegistry registry) {
        return new CountedAspect(registry);
    }
}
