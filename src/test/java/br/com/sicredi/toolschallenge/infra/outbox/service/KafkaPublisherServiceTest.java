package br.com.sicredi.toolschallenge.infra.outbox.service;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaPublisherService - Testes Unitários")
class KafkaPublisherServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private KafkaPublisherService service;

    @Test
    @DisplayName("Deve publicar evento no Kafka com sucesso")
    @SuppressWarnings("unchecked")
    void devePublicarEventoNoKafkaComSucesso() throws Exception {
        // Arrange
        OutboxEvento evento = criarEvento(1L, "PAG-123", "PagamentoAutorizado");
        String mensagemJson = "{\"eventoId\":1,\"eventoTipo\":\"PagamentoAutorizado\"}";
        
        CompletableFuture<SendResult<String, String>> futureSuccess = criarFutureSuccess();

        when(objectMapper.writeValueAsString(any())).thenReturn(mensagemJson);
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(futureSuccess);

        // Act
        CompletableFuture<SendResult<String, String>> resultado = service.publicarEvento(evento);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isCompletedWithValueMatching(result -> result != null);

        verify(objectMapper).writeValueAsString(argThat(msg -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> mensagem = (Map<String, Object>) msg;
            return mensagem.get("eventoId").equals(1L) &&
                   mensagem.get("eventoTipo").equals("PagamentoAutorizado") &&
                   mensagem.get("agregadoId").equals("PAG-123") &&
                   mensagem.get("agregadoTipo").equals("Pagamento");
        }));
        
        verify(kafkaTemplate).send(eq("pagamentos"), eq("PAG-123"), eq(mensagemJson));
    }

    @Test
    @DisplayName("Deve usar agregadoId como chave de particionamento")
    @SuppressWarnings("unchecked")
    void deveUsarAgregadoIdComoChaveDeParticionamento() throws Exception {
        // Arrange
        OutboxEvento evento = criarEvento(2L, "EST-456", "EstornoCancelado");
        CompletableFuture<SendResult<String, String>> futureSuccess = criarFutureSuccess();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(futureSuccess);

        // Act
        service.publicarEvento(evento);

        // Assert
        ArgumentCaptor<String> chaveCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), chaveCaptor.capture(), anyString());
        
        assertThat(chaveCaptor.getValue()).isEqualTo("EST-456");
    }

    @Test
    @DisplayName("Deve publicar evento no tópico correto")
    @SuppressWarnings("unchecked")
    void devePublicarEventoNoTopicoCorreto() throws Exception {
        // Arrange
        OutboxEvento evento = criarEvento(3L, "PAG-789", "PagamentoNegado");
        evento.setTopicoKafka("pagamento.eventos");
        
        CompletableFuture<SendResult<String, String>> futureSuccess = criarFutureSuccess();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(futureSuccess);

        // Act
        service.publicarEvento(evento);

        // Assert
        ArgumentCaptor<String> topicoCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(topicoCaptor.capture(), anyString(), anyString());
        
        assertThat(topicoCaptor.getValue()).isEqualTo("pagamento.eventos");
    }

    @Test
    @DisplayName("Deve construir mensagem com todos os metadados")
    @SuppressWarnings("unchecked")
    void deveConstruirMensagemComTodosOsMetadados() throws Exception {
        // Arrange
        OutboxEvento evento = criarEvento(4L, "PAG-111", "PagamentoAutorizado");
        CompletableFuture<SendResult<String, String>> futureSuccess = criarFutureSuccess();

        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(futureSuccess);

        // Act
        service.publicarEvento(evento);

        // Assert
        verify(objectMapper).writeValueAsString(argThat(msg -> {
            @SuppressWarnings("unchecked")
            Map<String, Object> mensagem = (Map<String, Object>) msg;
            
            return mensagem.containsKey("eventoId") &&
                   mensagem.containsKey("eventoTipo") &&
                   mensagem.containsKey("agregadoId") &&
                   mensagem.containsKey("agregadoTipo") &&
                   mensagem.containsKey("timestamp") &&
                   mensagem.containsKey("payload") &&
                   mensagem.get("payload") instanceof Map;
        }));
    }

    @Test
    @DisplayName("Deve retornar CompletableFuture com exceção quando falha serialização")
    void deveRetornarFutureComExcecaoQuandoFalhaSerializacao() throws Exception {
        // Arrange
        OutboxEvento evento = criarEvento(5L, "PAG-222", "PagamentoAutorizado");
        
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Erro de serialização"));

        // Act
        CompletableFuture<SendResult<String, String>> resultado = service.publicarEvento(evento);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.isCompletedExceptionally()).isTrue();
        
        verify(objectMapper).writeValueAsString(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Deve publicar evento de teste com sucesso")
    @SuppressWarnings("unchecked")
    void devePublicarEventoDeTesteComSucesso() throws Exception {
        // Arrange
        String topico = "teste.eventos";
        String chave = "TESTE-123";
        Map<String, Object> mensagem = Map.of("campo", "valor");
        String mensagemJson = "{\"campo\":\"valor\"}";
        
        CompletableFuture<SendResult<String, String>> futureSuccess = criarFutureSuccess();

        when(objectMapper.writeValueAsString(mensagem)).thenReturn(mensagemJson);
        when(kafkaTemplate.send(topico, chave, mensagemJson)).thenReturn(futureSuccess);

        // Act
        service.publicarEventoTeste(topico, chave, mensagem);

        // Assert
        verify(objectMapper).writeValueAsString(mensagem);
        verify(kafkaTemplate).send(topico, chave, mensagemJson);
    }

    @Test
    @DisplayName("Não deve falhar publicação de teste quando erro de serialização")
    void naoDeveFalharPublicacaoDeTesteQuandoErroDeSerializacao() throws Exception {
        // Arrange
        String topico = "teste.eventos";
        String chave = "TESTE-456";
        Map<String, Object> mensagem = Map.of("campo", "valor");
        
        when(objectMapper.writeValueAsString(mensagem)).thenThrow(new RuntimeException("Erro de serialização"));

        // Act - Não deve lançar exceção
        service.publicarEventoTeste(topico, chave, mensagem);

        // Assert
        verify(objectMapper).writeValueAsString(mensagem);
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    // Método auxiliar para criar eventos de teste
    private OutboxEvento criarEvento(Long id, String agregadoId, String eventoTipo) {
        return OutboxEvento.builder()
            .id(id)
            .agregadoId(agregadoId)
            .agregadoTipo("Pagamento")
            .eventoTipo(eventoTipo)
            .payload(Map.of("id", agregadoId, "valor", 100.50))
            .topicoKafka("pagamentos")
            .status("PENDENTE")
            .tentativas(0)
            .criadoEm(OffsetDateTime.now())
            .build();
    }
    
    // Método auxiliar para criar SendResult mockado com RecordMetadata
    @SuppressWarnings("unchecked")
    private CompletableFuture<SendResult<String, String>> criarFutureSuccess() {
        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata recordMetadata = mock(RecordMetadata.class);
        
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        when(recordMetadata.partition()).thenReturn(0);
        when(recordMetadata.offset()).thenReturn(123L);
        
        return CompletableFuture.completedFuture(sendResult);
    }
}
