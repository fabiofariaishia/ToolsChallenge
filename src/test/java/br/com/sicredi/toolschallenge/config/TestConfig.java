package br.com.sicredi.toolschallenge.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.mockito.Mockito.mock;

/**
 * Configuração de testes - desabilita Redis e Kafka para testes unitários.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
	RedisAutoConfiguration.class,
	RedisRepositoriesAutoConfiguration.class,
	KafkaAutoConfiguration.class
})
public class TestConfig {

	/**
	 * Mock do RedisConnectionFactory para evitar tentativa de conexão real nos testes
	 */
	@Bean
	public RedisConnectionFactory redisConnectionFactory() {
		return mock(RedisConnectionFactory.class);
	}
}
