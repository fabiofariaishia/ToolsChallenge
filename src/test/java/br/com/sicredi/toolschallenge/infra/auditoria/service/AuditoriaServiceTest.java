package br.com.sicredi.toolschallenge.infra.auditoria.service;

import br.com.sicredi.toolschallenge.infra.auditoria.EventoAuditoria;
import br.com.sicredi.toolschallenge.infra.auditoria.repository.EventoAuditoriaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditoriaService - Testes Unitários")
class AuditoriaServiceTest {

    @Mock
    private EventoAuditoriaRepository repository;

    @InjectMocks
    private AuditoriaService service;

    @Test
    @DisplayName("Deve registrar evento de auditoria com sucesso")
    void deveRegistrarEventoDeAuditoriaComSucesso() {
        // Arrange
        String agregadoTipo = "PAGAMENTO";
        String agregadoId = "PAG-123";
        String eventoTipo = "PAGAMENTO_CRIADO";
        Map<String, Object> payload = Map.of("valor", 100.50, "status", "AUTORIZADO");

        EventoAuditoria eventoEsperado = EventoAuditoria.builder()
            .id(1L)
            .agregadoTipo(agregadoTipo)
            .agregadoId(agregadoId)
            .eventoTipo(eventoTipo)
            .dados(payload)
            .build();

        when(repository.save(any(EventoAuditoria.class))).thenReturn(eventoEsperado);

        // Act
        service.registrarEvento(agregadoTipo, agregadoId, eventoTipo, payload);

        // Assert
        verify(repository).save(argThat(evento ->
            evento.getAgregadoTipo().equals(agregadoTipo) &&
            evento.getAgregadoId().equals(agregadoId) &&
            evento.getEventoTipo().equals(eventoTipo) &&
            evento.getDados().equals(payload) &&
            evento.getUsuario().equals("SISTEMA")
        ));
    }

    @Test
    @DisplayName("Deve registrar evento com metadados")
    void deveRegistrarEventoComMetadados() {
        // Arrange
        String agregadoTipo = "ESTORNO";
        String agregadoId = "EST-456";
        String eventoTipo = "ESTORNO_PROCESSADO";
        Map<String, Object> payload = Map.of("valorEstornado", 50.25);
        Map<String, Object> metadados = Map.of("ip", "192.168.1.1", "userAgent", "Mozilla");

        EventoAuditoria eventoEsperado = EventoAuditoria.builder()
            .id(2L)
            .agregadoTipo(agregadoTipo)
            .agregadoId(agregadoId)
            .eventoTipo(eventoTipo)
            .dados(payload)
            .metadados(metadados)
            .build();

        when(repository.save(any(EventoAuditoria.class))).thenReturn(eventoEsperado);

        // Act
        service.registrarEventoComMetadados(agregadoTipo, agregadoId, eventoTipo, payload, metadados);

        // Assert
        verify(repository).save(argThat(evento ->
            evento.getMetadados() != null &&
            evento.getMetadados().equals(metadados)
        ));
    }

    @Test
    @DisplayName("Não deve propagar exceção se falhar ao salvar auditoria")
    void naoDevePropagaExcecaoSeFalharAoSalvar() {
        // Arrange
        String agregadoTipo = "PAGAMENTO";
        String agregadoId = "PAG-789";
        String eventoTipo = "PAGAMENTO_FALHOU";
        Map<String, Object> payload = Map.of("erro", "Timeout");

        when(repository.save(any(EventoAuditoria.class)))
            .thenThrow(new RuntimeException("Database error"));

        // Act - Não deve lançar exceção
        service.registrarEvento(agregadoTipo, agregadoId, eventoTipo, payload);

        // Assert - Apenas verificar que tentou salvar
        verify(repository).save(any(EventoAuditoria.class));
    }

    @Test
    @DisplayName("Deve buscar histórico de auditoria por agregado")
    void deveBuscarHistoricoPorAgregado() {
        // Arrange
        String agregadoTipo = "PAGAMENTO";
        String agregadoId = "PAG-123";
        
        EventoAuditoria evento1 = criarEvento(1L, agregadoTipo, agregadoId, "CRIADO");
        EventoAuditoria evento2 = criarEvento(2L, agregadoTipo, agregadoId, "AUTORIZADO");
        List<EventoAuditoria> historico = List.of(evento2, evento1);  // Ordem DESC

        when(repository.findByAgregadoTipoAndAgregadoIdOrderByCriadoEmDesc(agregadoTipo, agregadoId))
            .thenReturn(historico);

        // Act
        List<EventoAuditoria> resultado = service.buscarHistorico(agregadoTipo, agregadoId);

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(evento2, evento1);
        
        verify(repository).findByAgregadoTipoAndAgregadoIdOrderByCriadoEmDesc(agregadoTipo, agregadoId);
    }

    @Test
    @DisplayName("Deve buscar eventos por tipo")
    void deveBuscarEventosPorTipo() {
        // Arrange
        String eventoTipo = "PAGAMENTO_AUTORIZADO";
        
        EventoAuditoria evento1 = criarEvento(1L, "PAGAMENTO", "PAG-001", eventoTipo);
        EventoAuditoria evento2 = criarEvento(2L, "PAGAMENTO", "PAG-002", eventoTipo);
        List<EventoAuditoria> eventos = List.of(evento2, evento1);

        when(repository.findByEventoTipoOrderByCriadoEmDesc(eventoTipo)).thenReturn(eventos);

        // Act
        List<EventoAuditoria> resultado = service.buscarPorTipoEvento(eventoTipo);

        // Assert
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(evento2, evento1);
        
        verify(repository).findByEventoTipoOrderByCriadoEmDesc(eventoTipo);
    }

    @Test
    @DisplayName("Deve buscar eventos por período")
    void deveBuscarEventosPorPeriodo() {
        // Arrange
        OffsetDateTime dataInicio = OffsetDateTime.now().minusDays(7);
        OffsetDateTime dataFim = OffsetDateTime.now();
        
        EventoAuditoria evento = criarEvento(1L, "PAGAMENTO", "PAG-001", "CRIADO");
        List<EventoAuditoria> eventos = List.of(evento);

        when(repository.findByPeriodo(dataInicio, dataFim)).thenReturn(eventos);

        // Act
        List<EventoAuditoria> resultado = service.buscarPorPeriodo(dataInicio, dataFim);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado).containsExactly(evento);
        
        verify(repository).findByPeriodo(dataInicio, dataFim);
    }

    @Test
    @DisplayName("Deve buscar últimos eventos com limite")
    void deveBuscarUltimosEventosComLimite() {
        // Arrange
        int quantidade = 10;
        
        EventoAuditoria evento = criarEvento(1L, "PAGAMENTO", "PAG-001", "CRIADO");
        List<EventoAuditoria> eventos = List.of(evento);

        when(repository.findUltimosEventos(any(PageRequest.class))).thenReturn(eventos);

        // Act
        List<EventoAuditoria> resultado = service.buscarUltimosEventos(quantidade);

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0)).isEqualTo(evento);
        
        verify(repository).findUltimosEventos(argThat(pageable -> 
            pageable.getPageSize() == quantidade && 
            pageable.getPageNumber() == 0
        ));
    }

    @Test
    @DisplayName("Deve obter estatísticas de auditoria")
    void deveObterEstatisticasDeAuditoria() {
        // Arrange
        when(repository.count()).thenReturn(100L);
        when(repository.countByAgregadoTipo("PAGAMENTO")).thenReturn(70L);
        when(repository.countByAgregadoTipo("ESTORNO")).thenReturn(30L);

        // Act
        Map<String, Long> estatisticas = service.obterEstatisticas();

        // Assert
        assertThat(estatisticas)
            .containsEntry("totalEventos", 100L)
            .containsEntry("eventosPagamento", 70L)
            .containsEntry("eventosEstorno", 30L);
        
        verify(repository).count();
        verify(repository).countByAgregadoTipo("PAGAMENTO");
        verify(repository).countByAgregadoTipo("ESTORNO");
    }

    @Test
    @DisplayName("Deve limpar eventos antigos")
    void deveLimparEventosAntigos() {
        // Arrange
        int diasRetencao = 90;

        // Act
        service.limparEventosAntigos(diasRetencao);

        // Assert
        verify(repository).deleteEventosAntigos(any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Deve calcular data limite corretamente ao limpar eventos antigos")
    void deveCalcularDataLimiteCorretamenteAoLimpar() {
        // Arrange
        int diasRetencao = 30;
        OffsetDateTime agora = OffsetDateTime.now();

        // Act
        service.limparEventosAntigos(diasRetencao);

        // Assert
        verify(repository).deleteEventosAntigos(argThat(dataLimite -> {
            long diffDays = java.time.Duration.between(dataLimite, agora).toDays();
            return diffDays >= 29 && diffDays <= 30;  // Aproximadamente 30 dias
        }));
    }

    // Método auxiliar para criar eventos de teste
    private EventoAuditoria criarEvento(Long id, String agregadoTipo, String agregadoId, String eventoTipo) {
        return EventoAuditoria.builder()
            .id(id)
            .agregadoTipo(agregadoTipo)
            .agregadoId(agregadoId)
            .eventoTipo(eventoTipo)
            .dados(Map.of("id", agregadoId))
            .usuario("SISTEMA")
            .criadoEm(OffsetDateTime.now())
            .build();
    }
}
