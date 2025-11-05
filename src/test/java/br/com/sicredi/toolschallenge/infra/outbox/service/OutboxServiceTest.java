package br.com.sicredi.toolschallenge.infra.outbox.service;

import br.com.sicredi.toolschallenge.infra.outbox.OutboxEvento;
import br.com.sicredi.toolschallenge.infra.outbox.repository.OutboxEventoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxService - Testes Unitários")
class OutboxServiceTest {

    @Mock
    private OutboxEventoRepository repository;

    @InjectMocks
    private OutboxService service;

    @Test
    @DisplayName("Deve criar evento no outbox com sucesso")
    void deveCriarEventoNoOutboxComSucesso() {
        // Arrange
        String agregadoId = "PAG-123";
        String agregadoTipo = "Pagamento";
        String eventoTipo = "PagamentoAutorizado";
        Map<String, Object> payload = Map.of("valor", 100.50, "status", "AUTORIZADO");
        String topicoKafka = "pagamentos";

        OutboxEvento eventoEsperado = OutboxEvento.builder()
            .id(1L)
            .agregadoId(agregadoId)
            .agregadoTipo(agregadoTipo)
            .eventoTipo(eventoTipo)
            .payload(payload)
            .topicoKafka(topicoKafka)
            .status("PENDENTE")
            .tentativas(0)
            .build();

        when(repository.save(any(OutboxEvento.class))).thenReturn(eventoEsperado);

        // Act
        OutboxEvento resultado = service.criarEvento(agregadoId, agregadoTipo, eventoTipo, payload, topicoKafka);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getAgregadoId()).isEqualTo(agregadoId);
        assertThat(resultado.getAgregadoTipo()).isEqualTo(agregadoTipo);
        assertThat(resultado.getEventoTipo()).isEqualTo(eventoTipo);
        assertThat(resultado.getStatus()).isEqualTo("PENDENTE");
        assertThat(resultado.getTentativas()).isZero();

        verify(repository).save(argThat(evento -> 
            evento.getAgregadoId().equals(agregadoId) &&
            evento.getStatus().equals("PENDENTE") &&
            evento.getTentativas() == 0
        ));
    }

    @Test
    @DisplayName("Deve buscar eventos pendentes")
    void deveBuscarEventosPendentes() {
        // Arrange
        OutboxEvento evento1 = criarEvento(1L, "PAG-001", "PENDENTE");
        OutboxEvento evento2 = criarEvento(2L, "PAG-002", "PENDENTE");
        List<OutboxEvento> eventosPendentes = List.of(evento1, evento2);

        when(repository.findEventosPendentes()).thenReturn(eventosPendentes);

        // Act
        List<OutboxEvento> resultado = service.buscarEventosPendentes();

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(evento1, evento2);
        
        verify(repository).findEventosPendentes();
    }

    @Test
    @DisplayName("Deve buscar eventos pendentes com limite")
    void deveBuscarEventosPendentesComLimite() {
        // Arrange
        int limit = 10;
        OutboxEvento evento = criarEvento(1L, "PAG-001", "PENDENTE");
        List<OutboxEvento> eventosPendentes = List.of(evento);

        when(repository.findEventosPendentes(limit)).thenReturn(eventosPendentes);

        // Act
        List<OutboxEvento> resultado = service.buscarEventosPendentes(limit);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0)).isEqualTo(evento);
        
        verify(repository).findEventosPendentes(limit);
    }

    @Test
    @DisplayName("Deve buscar eventos para retry")
    void deveBuscarEventosParaRetry() {
        // Arrange
        OutboxEvento evento = criarEvento(1L, "PAG-001", "ERRO");
        List<OutboxEvento> eventosParaRetry = List.of(evento);

        when(repository.findEventosParaRetry()).thenReturn(eventosParaRetry);

        // Act
        List<OutboxEvento> resultado = service.buscarEventosParaRetry();

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0)).isEqualTo(evento);
        
        verify(repository).findEventosParaRetry();
    }

    @Test
    @DisplayName("Deve marcar evento como processado")
    void deveMarcarEventoComoProcessado() {
        // Arrange
        Long eventoId = 123L;
        OutboxEvento evento = criarEvento(eventoId, "PAG-001", "PENDENTE");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(repository.save(any(OutboxEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.marcarComoProcessado(eventoId);

        // Assert
        verify(repository).findById(eventoId);
        verify(repository).save(argThat(e -> 
            e.getStatus().equals("PROCESSADO") &&
            e.getProcessadoEm() != null
        ));
    }

    @Test
    @DisplayName("Não deve falhar ao marcar evento inexistente como processado")
    void naoDeveFalharAoMarcarEventoInexistenteComoProcessado() {
        // Arrange
        Long eventoId = 999L;

        when(repository.findById(eventoId)).thenReturn(Optional.empty());

        // Act
        service.marcarComoProcessado(eventoId);

        // Assert
        verify(repository).findById(eventoId);
        verify(repository, never()).save(any(OutboxEvento.class));
    }

    @Test
    @DisplayName("Deve marcar evento como erro")
    void deveMarcarEventoComoErro() {
        // Arrange
        Long eventoId = 456L;
        String mensagemErro = "Kafka timeout";
        OutboxEvento evento = criarEvento(eventoId, "EST-001", "PENDENTE");

        when(repository.findById(eventoId)).thenReturn(Optional.of(evento));
        when(repository.save(any(OutboxEvento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.marcarComoErro(eventoId, mensagemErro);

        // Assert
        verify(repository).findById(eventoId);
        verify(repository).save(argThat(e -> 
            e.getStatus().equals("ERRO") &&
            e.getUltimoErro().equals(mensagemErro) &&
            e.getTentativas() == 1
        ));
    }

    @Test
    @DisplayName("Deve limpar eventos processados antigos")
    void deveLimparEventosProcessadosAntigos() {
        // Arrange
        int diasRetencao = 30;
        int quantidadeRemovida = 42;

        when(repository.deleteEventosProcessadosAntigos(any(OffsetDateTime.class)))
            .thenReturn(quantidadeRemovida);

        // Act
        int resultado = service.limparEventosProcessadosAntigos(diasRetencao);

        // Assert
        assertThat(resultado).isEqualTo(42);
        
        verify(repository).deleteEventosProcessadosAntigos(any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Deve buscar eventos por agregado")
    void deveBuscarEventosPorAgregado() {
        // Arrange
        String agregadoId = "PAG-123";
        OutboxEvento evento1 = criarEvento(1L, agregadoId, "PROCESSADO");
        OutboxEvento evento2 = criarEvento(2L, agregadoId, "PENDENTE");
        List<OutboxEvento> eventos = List.of(evento1, evento2);

        when(repository.findByAgregadoId(agregadoId)).thenReturn(eventos);

        // Act
        List<OutboxEvento> resultado = service.buscarEventosPorAgregado(agregadoId);

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(evento1, evento2);
        
        verify(repository).findByAgregadoId(agregadoId);
    }

    @Test
    @DisplayName("Deve contar eventos pendentes")
    void deveContarEventosPendentes() {
        // Arrange
        Long quantidadeEsperada = 15L;

        when(repository.countEventosPendentes()).thenReturn(quantidadeEsperada);

        // Act
        Long resultado = service.contarEventosPendentes();

        // Assert
        assertThat(resultado).isEqualTo(15L);
        
        verify(repository).countEventosPendentes();
    }

    @Test
    @DisplayName("Deve contar eventos com erro")
    void deveContarEventosComErro() {
        // Arrange
        Long quantidadeEsperada = 3L;

        when(repository.countEventosComErro()).thenReturn(quantidadeEsperada);

        // Act
        Long resultado = service.contarEventosComErro();

        // Assert
        assertThat(resultado).isEqualTo(3L);
        
        verify(repository).countEventosComErro();
    }

    // Método auxiliar para criar eventos de teste
    private OutboxEvento criarEvento(Long id, String agregadoId, String status) {
        return OutboxEvento.builder()
            .id(id)
            .agregadoId(agregadoId)
            .agregadoTipo("Pagamento")
            .eventoTipo("PagamentoAutorizado")
            .payload(Map.of("id", agregadoId))
            .topicoKafka("pagamentos")
            .status(status)
            .tentativas(0)
            .build();
    }
}
