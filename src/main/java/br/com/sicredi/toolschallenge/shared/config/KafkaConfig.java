package br.com.sicredi.toolschallenge.shared.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração do Kafka Producer.
 * 
 * Configurações otimizadas para o Outbox Pattern:
 * - acks=all: Garante que mensagem foi recebida por todos os replicas
 * - retries=3: Tenta reenviar em caso de erro temporário
 * - enable.idempotence=true: Previne duplicatas no Kafka
 * - max.in.flight.requests.per.connection=1: Garante ordem das mensagens
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Endereço do Kafka
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serializers
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Configurações de confiabilidade
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // Aguarda confirmação de todos os replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); // Retry automático
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Previne duplicatas
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1); // Garante ordem
        
        // Configurações de performance
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // Batch de 16KB
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10); // Aguarda 10ms antes de enviar
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // Buffer de 32MB
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compressão
        
        // Timeouts
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 segundos
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutos
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
