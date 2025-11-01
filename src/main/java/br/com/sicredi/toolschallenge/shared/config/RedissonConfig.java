package br.com.sicredi.toolschallenge.shared.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Redisson para Lock Distribuído.
 * 
 * Usado para:
 * - Lock distribuído em estornos (prevenir duplicação)
 * - Sincronização distribuída entre múltiplas instâncias da aplicação
 * 
 * Features:
 * - Watchdog: Renova lock automaticamente a cada 10s
 * - Timeout: Lock expira em 30s se app crashar (evita deadlock)
 * - Retry: 3 tentativas com intervalo de 1.5s em caso de falha
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:redis123}")
    private String redisPassword;

    /**
     * Cria cliente Redisson configurado.
     * 
     * Configurações importantes:
     * - connectionPoolSize: 10 conexões simultâneas
     * - connectionMinimumIdleSize: 5 conexões idle mínimas
     * - timeout: 3s para comandos Redis
     * - retryAttempts: 3 tentativas em caso de falha
     * - retryInterval: 1.5s entre tentativas
     * 
     * @return Cliente Redisson configurado
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = true)
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // Configuração para servidor único (desenvolvimento/produção simples)
        var serverConfig = config.useSingleServer()
            .setAddress("redis://" + redisHost + ":" + redisPort)
            .setConnectionPoolSize(10)           // Pool de conexões
            .setConnectionMinimumIdleSize(5)     // Mínimo de conexões idle
            .setTimeout(3000)                    // Timeout de comando (3s)
            .setRetryAttempts(3)                 // Retry em caso de falha
            .setRetryInterval(1500);             // Intervalo entre retries (1.5s)
        
        // Só configurar senha se não for vazia
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            serverConfig.setPassword(redisPassword);
        }
        
        return Redisson.create(config);
    }
}
