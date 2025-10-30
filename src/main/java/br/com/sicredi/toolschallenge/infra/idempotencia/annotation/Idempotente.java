package br.com.sicredi.toolschallenge.infra.idempotencia.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * Annotation para habilitar controle de idempotência em métodos de controller.
 * 
 * Funcionamento:
 * 1. Cliente envia header "Chave-Idempotencia" (obrigatório)
 * 2. Sistema verifica se chave já foi processada (Redis → PostgreSQL)
 * 3. Se já processada: retorna resposta anterior (header X-Idempotency-Replayed: true)
 * 4. Se nova: processa normalmente e armazena resposta
 * 
 * Exemplo:
 * <pre>
 * {@code @Idempotente}(ttl = 24, unidadeTempo = TimeUnit.HOURS, headerName = "Chave-Idempotencia")
 * public ResponseEntity<?> criarPagamento(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotente {
    
    /**
     * Tempo de vida da chave de idempotência.
     * Após esse período, a chave expira e pode ser reprocessada.
     * 
     * @return tempo de vida (padrão: 24)
     */
    long ttl() default 24;
    
    /**
     * Unidade de tempo para o TTL.
     * 
     * @return unidade de tempo (padrão: HOURS)
     */
    TimeUnit unidadeTempo() default TimeUnit.HOURS;
    
    /**
     * Nome do header HTTP que contém a chave de idempotência.
     * 
     * @return nome do header (padrão: "Chave-Idempotencia")
     */
    String headerName() default "Chave-Idempotencia";
}
