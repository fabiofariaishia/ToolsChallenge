package br.com.sicredi.toolschallenge.infra.idempotencia.service;

import br.com.sicredi.toolschallenge.infra.idempotencia.Idempotencia;
import br.com.sicredi.toolschallenge.infra.idempotencia.repository.IdempotenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service para gerenciar idempotência usando cache em duas camadas:
 * - L1 (Redis): Cache rápido e distribuído
 * - L2 (PostgreSQL): Persistência durável
 * 
 * Fluxo de leitura:
 * 1. Tenta buscar no Redis (L1)
 * 2. Se não encontrar, busca no PostgreSQL (L2)
 * 3. Se encontrar no PostgreSQL, recarrega no Redis
 * 
 * Fluxo de escrita:
 * 1. Salva no PostgreSQL (L2 - durável)
 * 2. Salva no Redis (L1 - rápido)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotenciaService {

    private final IdempotenciaRepository repository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "idempotencia:";

    /**
     * Busca resposta armazenada para uma chave de idempotência.
     * Implementa cache em duas camadas (L1: Redis, L2: PostgreSQL).
     * 
     * @param chave Chave de idempotência
     * @return Optional com a resposta se encontrada e válida
     */
    public Optional<RespostaIdempotente> buscarResposta(String chave) {
        log.debug("Buscando resposta para chave: {}", chave);

        // L1: Tentar buscar no Redis
        RespostaIdempotente respostaRedis = buscarNoRedis(chave);
        if (respostaRedis != null) {
            log.info("Resposta encontrada no Redis para chave: {}", chave);
            return Optional.of(respostaRedis);
        }

        // L2: Se não encontrou no Redis, buscar no PostgreSQL
        Optional<Idempotencia> registroOpt = repository.findChaveValida(chave, OffsetDateTime.now());
        
        if (registroOpt.isPresent()) {
            Idempotencia registro = registroOpt.get();
            log.info("Resposta encontrada no PostgreSQL para chave: {}", chave);
            
            // Recarregar no Redis para próximas consultas
            RespostaIdempotente resposta = new RespostaIdempotente(registro.getResponseBody(), registro.getStatusHttp());
            recarregarNoRedis(chave, resposta, registro.getExpiraEm());
            
            return Optional.of(resposta);
        }

        log.debug("Nenhuma resposta encontrada para chave: {}", chave);
        return Optional.empty();
    }

    /**
     * Salva resposta em ambas as camadas de cache.
     * 
     * @param chave Chave de idempotência
     * @param idTransacao ID da transação processada
     * @param endpoint Endpoint processado
     * @param resposta Corpo da resposta
     * @param statusHttp Status HTTP da resposta
     * @param ttl Tempo de vida em segundos
     */
    @Transactional
    public void salvarResposta(String chave, String idTransacao, String endpoint, Map<String, Object> resposta, Integer statusHttp, long ttl) {
        log.debug("Salvando resposta para chave: {} com TTL: {}s", chave, ttl);

        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime expiraEm = agora.plusSeconds(ttl);

        // L2: Salvar no PostgreSQL (persistência durável)
        Idempotencia registro = Idempotencia.builder()
                .chave(chave)
                .idTransacao(idTransacao)
                .endpoint(endpoint)
                .statusHttp(statusHttp)
                .responseBody(resposta)
                .criadoEm(agora)
                .expiraEm(expiraEm)
                .build();

        repository.save(registro);
        log.debug("Resposta salva no PostgreSQL para chave: {}", chave);

        // L1: Salvar no Redis (cache rápido)
        salvarNoRedis(chave, new RespostaIdempotente(resposta, statusHttp), ttl);
    }

    /**
     * Limpa registros expirados do banco de dados.
     * Executado por job agendado.
     * 
     * @return Quantidade de registros removidos
     */
    @Transactional
    public int limparRegistrosExpirados() {
        OffsetDateTime agora = OffsetDateTime.now();
        
        Long countExpirados = repository.countChavesExpiradas(agora);
        log.debug("Encontrados {} registros expirados para limpeza", countExpirados);
        
        if (countExpirados > 0) {
            int removidos = repository.deleteChavesExpiradas(agora);
            log.info("Removidos {} registros expirados de idempotência", removidos);
            return removidos;
        }
        
        return 0;
    }

    /**
     * Busca resposta no Redis (L1 cache).
     */
    private RespostaIdempotente buscarNoRedis(String chave) {
        try {
            String redisKey = REDIS_PREFIX + chave;
            Object value = redisTemplate.opsForValue().get(redisKey);
            
            if (value != null) {
                String json = objectMapper.writeValueAsString(value);
                return objectMapper.readValue(json, RespostaIdempotente.class);
            }
        } catch (Exception e) {
            log.warn("Erro ao buscar no Redis para chave: {}. Fallback para PostgreSQL", chave, e);
        }
        return null;
    }

    /**
     * Salva resposta no Redis (L1 cache).
     */
    private void salvarNoRedis(String chave, RespostaIdempotente resposta, long ttlSeconds) {
        try {
            String redisKey = REDIS_PREFIX + chave;
            redisTemplate.opsForValue().set(redisKey, resposta, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Resposta salva no Redis para chave: {} com TTL: {}s", chave, ttlSeconds);
        } catch (Exception e) {
            log.warn("Erro ao salvar no Redis para chave: {}, mas salvo no PostgreSQL com sucesso", chave, e);
        }
    }

    /**
     * Recarrega resposta no Redis após busca no PostgreSQL.
     */
    private void recarregarNoRedis(String chave, RespostaIdempotente resposta, OffsetDateTime expiraEm) {
        try {
            long ttlSeconds = OffsetDateTime.now().until(expiraEm, java.time.temporal.ChronoUnit.SECONDS);
            if (ttlSeconds > 0) {
                salvarNoRedis(chave, resposta, ttlSeconds);
            }
        } catch (Exception e) {
            log.warn("Erro ao recarregar no Redis para chave: {}", chave, e);
        }
    }

    /**
     * Classe interna para representar resposta armazenada.
     */
    public record RespostaIdempotente(Map<String, Object> corpo, Integer statusHttp) {}
}
