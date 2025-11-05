package br.com.sicredi.toolschallenge.infra.idempotencia.service;

import br.com.sicredi.toolschallenge.infra.idempotencia.Idempotencia;
import br.com.sicredi.toolschallenge.infra.idempotencia.repository.IdempotenciaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotenciaService - Testes Unitários")
class IdempotenciaServiceTest {

    @Mock
    private IdempotenciaRepository repository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private IdempotenciaService service;

    @Test
    @DisplayName("Deve salvar resposta no PostgreSQL e Redis com sucesso")
    void deveSalvarRespostaComSucesso() {
        // Arrange
        String chave = "test-key-123";
        String idTransacao = "txn-001";
        String endpoint = "/api/pagamentos";
        Map<String, Object> resposta = Map.of("id", "123", "status", "APROVADO");
        Integer statusHttp = 201;
        long ttl = 3600;
        
        // Configurar Redis
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        when(repository.save(any(Idempotencia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.salvarResposta(chave, idTransacao, endpoint, resposta, statusHttp, ttl);

        // Assert
        verify(repository).save(argThat(registro -> 
            registro.getChave().equals(chave) &&
            registro.getIdTransacao().equals(idTransacao) &&
            registro.getEndpoint().equals(endpoint) &&
            registro.getStatusHttp().equals(statusHttp) &&
            registro.getResponseBody().equals(resposta)
        ));
        
        verify(valueOperations).set(
            eq("idempotencia:" + chave),
            any(IdempotenciaService.RespostaIdempotente.class),
            eq(ttl),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve buscar resposta do Redis quando existir (L1 cache hit)")
    void deveBuscarRespostaDoRedisQuandoExistir() throws Exception {
        // Arrange
        String chave = "test-key-redis";
        Map<String, Object> corpo = Map.of("id", "456", "valor", 100.50);
        IdempotenciaService.RespostaIdempotente respostaEsperada = 
            new IdempotenciaService.RespostaIdempotente(corpo, 200);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotencia:" + chave)).thenReturn(respostaEsperada);
        
        // Mockar serialização/desserialização JSON
        String json = "{\"corpo\":{\"id\":\"456\",\"valor\":100.50},\"statusHttp\":200}";
        when(objectMapper.writeValueAsString(respostaEsperada)).thenReturn(json);
        when(objectMapper.readValue(json, IdempotenciaService.RespostaIdempotente.class))
            .thenReturn(respostaEsperada);

        // Act
        Optional<IdempotenciaService.RespostaIdempotente> resultado = service.buscarResposta(chave);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().corpo()).isEqualTo(corpo);
        assertThat(resultado.get().statusHttp()).isEqualTo(200);
        
        verify(valueOperations).get("idempotencia:" + chave);
        verify(repository, never()).findChaveValida(anyString(), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Deve buscar resposta do PostgreSQL quando não existir no Redis (L2 cache hit)")
    void deveBuscarRespostaDoPostgresQuandoNaoExistirNoRedis() {
        // Arrange
        String chave = "test-key-postgres";
        Map<String, Object> corpo = Map.of("id", "789");
        
        Idempotencia registro = Idempotencia.builder()
            .chave(chave)
            .responseBody(corpo)
            .statusHttp(200)
            .expiraEm(OffsetDateTime.now().plusHours(1))
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotencia:" + chave)).thenReturn(null);
        when(repository.findChaveValida(eq(chave), any(OffsetDateTime.class)))
            .thenReturn(Optional.of(registro));

        // Act
        Optional<IdempotenciaService.RespostaIdempotente> resultado = service.buscarResposta(chave);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().corpo()).isEqualTo(corpo);
        assertThat(resultado.get().statusHttp()).isEqualTo(200);
        
        verify(valueOperations).get("idempotencia:" + chave);
        verify(repository).findChaveValida(eq(chave), any(OffsetDateTime.class));
        
        // Deve recarregar no Redis
        verify(valueOperations).set(
            eq("idempotencia:" + chave),
            any(IdempotenciaService.RespostaIdempotente.class),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("Deve retornar vazio quando registro não existir em nenhum cache")
    void deveRetornarVazioQuandoRegistroNaoExiste() {
        // Arrange
        String chave = "chave-inexistente";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotencia:" + chave)).thenReturn(null);
        when(repository.findChaveValida(eq(chave), any(OffsetDateTime.class)))
            .thenReturn(Optional.empty());

        // Act
        Optional<IdempotenciaService.RespostaIdempotente> resultado = service.buscarResposta(chave);

        // Assert
        assertThat(resultado).isEmpty();
        
        verify(valueOperations).get("idempotencia:" + chave);
        verify(repository).findChaveValida(eq(chave), any(OffsetDateTime.class));
        verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("Deve limpar registros expirados com sucesso")
    void deveLimparRegistrosExpiradosComSucesso() {
        // Arrange
        int quantidadeRemovida = 15;

        when(repository.countChavesExpiradas(any(OffsetDateTime.class))).thenReturn(15L);
        when(repository.deleteChavesExpiradas(any(OffsetDateTime.class))).thenReturn(quantidadeRemovida);

        // Act
        int resultado = service.limparRegistrosExpirados();

        // Assert
        assertThat(resultado).isEqualTo(15);
        
        verify(repository).countChavesExpiradas(any(OffsetDateTime.class));
        verify(repository).deleteChavesExpiradas(any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Deve retornar zero quando não houver registros expirados para limpar")
    void deveRetornarZeroQuandoNaoHouverRegistrosExpirados() {
        // Arrange
        when(repository.countChavesExpiradas(any(OffsetDateTime.class))).thenReturn(0L);

        // Act
        int resultado = service.limparRegistrosExpirados();

        // Assert
        assertThat(resultado).isZero();
        
        verify(repository).countChavesExpiradas(any(OffsetDateTime.class));
        verify(repository, never()).deleteChavesExpiradas(any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Deve continuar salvando no PostgreSQL mesmo com erro no Redis")
    void deveContinuarSalvandoNoPostgresComErroNoRedis() {
        // Arrange
        String chave = "test-key-redis-error";
        String idTransacao = "txn-002";
        String endpoint = "/api/estornos";
        Map<String, Object> resposta = Map.of("id", "999");
        Integer statusHttp = 201;
        long ttl = 1800;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(repository.save(any(Idempotencia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("Redis indisponível")).when(valueOperations).set(
            anyString(), any(), anyLong(), any(TimeUnit.class)
        );

        // Act
        service.salvarResposta(chave, idTransacao, endpoint, resposta, statusHttp, ttl);

        // Assert
        verify(repository).save(any(Idempotencia.class)); // Postgres deve ter sido chamado
        verify(valueOperations).set(anyString(), any(), anyLong(), any(TimeUnit.class)); // Redis tentou salvar
    }

    @Test
    @DisplayName("Deve buscar do PostgreSQL quando Redis lançar exceção")
    void deveBuscarDoPostgresQuandoRedisLancarExcecao() {
        // Arrange
        String chave = "test-key-redis-exception";
        Map<String, Object> corpo = Map.of("id", "error-test");
        
        Idempotencia registro = Idempotencia.builder()
            .chave(chave)
            .responseBody(corpo)
            .statusHttp(200)
            .expiraEm(OffsetDateTime.now().plusHours(1))
            .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotencia:" + chave))
            .thenThrow(new RuntimeException("Redis connection timeout"));
        when(repository.findChaveValida(eq(chave), any(OffsetDateTime.class)))
            .thenReturn(Optional.of(registro));

        // Act
        Optional<IdempotenciaService.RespostaIdempotente> resultado = service.buscarResposta(chave);

        // Assert
        assertThat(resultado).isPresent();
        assertThat(resultado.get().corpo()).isEqualTo(corpo);
        
        verify(valueOperations).get("idempotencia:" + chave);
        verify(repository).findChaveValida(eq(chave), any(OffsetDateTime.class));
    }
}
