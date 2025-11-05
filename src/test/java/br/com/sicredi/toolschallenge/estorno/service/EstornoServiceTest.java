package br.com.sicredi.toolschallenge.estorno.service;

import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoResponse;
import br.com.sicredi.toolschallenge.adquirente.service.AdquirenteService;
import br.com.sicredi.toolschallenge.estorno.domain.Estorno;
import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import br.com.sicredi.toolschallenge.estorno.dto.DescricaoEstornoDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoMapper;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoRequestDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoResponseDTO;
import br.com.sicredi.toolschallenge.estorno.dto.TransacaoEstornoDTO;
import br.com.sicredi.toolschallenge.estorno.events.EstornoCriadoEvento;
import br.com.sicredi.toolschallenge.estorno.events.EstornoStatusAlteradoEvento;
import br.com.sicredi.toolschallenge.shared.config.ReprocessamentoProperties;
import org.springframework.context.ApplicationEventPublisher;
import br.com.sicredi.toolschallenge.infra.outbox.publisher.EventoPublisher;
import br.com.sicredi.toolschallenge.estorno.repository.EstornoRepository;
import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.repository.PagamentoRepository;
import br.com.sicredi.toolschallenge.shared.exception.NegocioException;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import br.com.sicredi.toolschallenge.adquirente.domain.StatusAutorizacao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para EstornoService.
 * 
 * Cobertura:
 * - Criação de estorno com sucesso (CANCELADO, NEGADO, PENDENTE)
 * - Validações de negócio (pagamento não encontrado, não AUTORIZADO, estorno parcial, duplicado, janela 24h)
 * - Integração com AdquirenteService
 * - Publicação de eventos (EstornoCriado, EstornoStatusAlterado)
 * - Métodos de consulta (buscarPorId, listarPorIdTransacao, listarEstornos, listarPorStatus)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EstornoService - Testes Unitários")
class EstornoServiceTest {

    @Mock
    private EstornoRepository repository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private EstornoMapper mapper;

    @Mock
    private EventoPublisher eventoPublisher;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AdquirenteService adquirenteService;

    @Mock
    private ReprocessamentoProperties reprocessamentoProperties;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter dlqCounter;

    @InjectMocks
    private EstornoService estornoService;

    private EstornoRequestDTO requestDTO;
    private Pagamento pagamento;
    private Estorno estorno;
    private AutorizacaoResponse autorizacaoAprovada;
    private AutorizacaoResponse autorizacaoNegada;
    private AutorizacaoResponse autorizacaoPendente;

    @BeforeEach
    void setUp() {
        // Request DTO
        requestDTO = new EstornoRequestDTO();
        requestDTO.setIdTransacao("TXN-123-TEST");
        requestDTO.setValor(new BigDecimal("150.00"));
        requestDTO.setMotivo("Cliente solicitou cancelamento");

        // Pagamento AUTORIZADO (criado há 2 horas - dentro da janela de 24h)
        pagamento = new Pagamento();
        pagamento.setId(1L);
        pagamento.setIdTransacao("TXN-123-TEST");
        pagamento.setValor(new BigDecimal("150.00"));
        pagamento.setStatus(StatusPagamento.AUTORIZADO);
        pagamento.setCriadoEm(OffsetDateTime.now().minusHours(2));
        pagamento.setDataHora(OffsetDateTime.now().minusHours(2)); // Para validação de janela 24h

        // Estorno inicial (PENDENTE)
        estorno = new Estorno();
        estorno.setIdTransacao("TXN-123-TEST");
        estorno.setValor(new BigDecimal("150.00"));
        estorno.setMotivo("Cliente solicitou cancelamento");
        estorno.setStatus(StatusEstorno.PENDENTE);
        estorno.setDataHora(OffsetDateTime.now()); // Timestamp do estorno

        // Respostas do adquirente
        autorizacaoAprovada = new AutorizacaoResponse(
            StatusAutorizacao.AUTORIZADO,
            "NSU123456",
            "AUTH001"
        );

        autorizacaoNegada = new AutorizacaoResponse(
            StatusAutorizacao.NEGADO,
            null,
            null
        );

        autorizacaoPendente = new AutorizacaoResponse(
            StatusAutorizacao.PENDENTE,
            null,
            null
        );
    }

    // ========== TESTE 1: CRIAÇÃO COM SUCESSO - STATUS CANCELADO ==========

    @Test
    @DisplayName("1. Deve criar estorno com sucesso - Status CANCELADO")
    void deveCriarEstornoComSucessoCancelado() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(false);
        when(mapper.paraEntidade(any(EstornoRequestDTO.class)))
            .thenReturn(estorno);
        when(repository.save(any(Estorno.class))).thenAnswer(invocation -> {
            Estorno e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada);
        
        // Construir DTO de resposta com nova estrutura aninhada
        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.CANCELADO.name())
            .nsu("NSU123456")
            .codigoAutorizacao("AUTH001")
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        EstornoResponseDTO resultado = estornoService.criarEstorno(requestDTO);

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado.getTransacao().getDescricao().getStatus()).isEqualTo(StatusEstorno.CANCELADO.name());
        assertThat(resultado.getTransacao().getDescricao().getNsu()).isEqualTo("NSU123456");
        assertThat(resultado.getTransacao().getDescricao().getCodigoAutorizacao()).isEqualTo("AUTH001");

        // Verificar interações
        verify(pagamentoRepository).findByIdTransacao("TXN-123-TEST");
        verify(repository).existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST");
        verify(repository, times(2)).save(any(Estorno.class)); // 1x PENDENTE + 1x CANCELADO
        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
        verify(eventoPublisher).publicarEstornoCriado(any());
        verify(eventoPublisher).publicarEstornoStatusAlterado(any());
    }

    // ========== TESTE 2: PAGAMENTO NÃO ENCONTRADO (404) ==========

    @Test
    @DisplayName("2. Deve lançar RecursoNaoEncontradoException quando pagamento não existe")
    void deveLancarExcecaoQuandoPagamentoNaoEncontrado() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-999-INEXISTENTE"))
            .thenReturn(Optional.empty());

        EstornoRequestDTO request = new EstornoRequestDTO();
        request.setIdTransacao("TXN-999-INEXISTENTE");
        request.setValor(new BigDecimal("100.00"));

        // Act & Assert
        assertThatThrownBy(() -> estornoService.criarEstorno(request))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("Pagamento");

        verify(pagamentoRepository).findByIdTransacao("TXN-999-INEXISTENTE");
    }

    // ========== TESTE 3: PAGAMENTO NÃO AUTORIZADO ==========

    @Test
    @DisplayName("3. Deve lançar NegocioException quando pagamento não está AUTORIZADO")
    void deveLancarExcecaoQuandoPagamentoNaoAutorizado() {
        // Arrange
        pagamento.setStatus(StatusPagamento.NEGADO);
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        // Act & Assert
        assertThatThrownBy(() -> estornoService.criarEstorno(requestDTO))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Apenas pagamentos AUTORIZADOS")
            .hasMessageContaining("NEGADO");

        verify(pagamentoRepository).findByIdTransacao("TXN-123-TEST");
    }

    // ========== TESTE 4: ESTORNO PARCIAL NÃO PERMITIDO ==========

    @Test
    @DisplayName("4. Deve lançar NegocioException quando valor de estorno é diferente do pagamento")
    void deveLancarExcecaoQuandoEstornoParcial() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        EstornoRequestDTO request = new EstornoRequestDTO();
        request.setIdTransacao("TXN-123-TEST");
        request.setValor(new BigDecimal("50.00")); // Metade do valor

        // Act & Assert
        assertThatThrownBy(() -> estornoService.criarEstorno(request))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Estorno parcial não permitido")
            .hasMessageContaining("150"); // Sem ".00" para aceitar formatação BR (150,00)

        verify(pagamentoRepository).findByIdTransacao("TXN-123-TEST");
    }

    // ========== TESTE 5: ESTORNO DUPLICADO ==========

    @Test
    @DisplayName("5. Deve lançar NegocioException quando já existe estorno CANCELADO")
    void deveLancarExcecaoQuandoJaExisteEstornoCancelado() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> estornoService.criarEstorno(requestDTO))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Já existe um estorno processado");

        verify(pagamentoRepository).findByIdTransacao("TXN-123-TEST");
        verify(repository).existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST");
    }

    // ========== TESTE 6: JANELA DE 24H EXPIRADA ==========

    @Test
    @DisplayName("6. Deve lançar NegocioException quando janela de 24h expirou")
    void deveLancarExcecaoQuandoJanelaExpirada() {
        // Arrange
        pagamento.setCriadoEm(OffsetDateTime.now().minusHours(25)); // 25 horas atrás
        pagamento.setDataHora(OffsetDateTime.now().minusHours(25)); // Para validação de janela
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        // Act & Assert
        assertThatThrownBy(() -> estornoService.criarEstorno(requestDTO))
            .isInstanceOf(NegocioException.class)
            .hasMessageContaining("Estorno só pode ser solicitado dentro de 24 horas");

        verify(pagamentoRepository).findByIdTransacao("TXN-123-TEST");
    }

    // ========== TESTE 7: MAPEAMENTO AUTORIZADO → CANCELADO ==========

    @Test
    @DisplayName("7. Deve mapear AUTORIZADO para CANCELADO quando adquirente aprova")
    void deveMapeiarAutorizadoParaCancelado() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(false);
        when(mapper.paraEntidade(any(EstornoRequestDTO.class)))
            .thenReturn(estorno);
        when(repository.save(any(Estorno.class))).thenAnswer(invocation -> {
            Estorno e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada);

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.CANCELADO.name())
            .nsu("NSU123456")
            .codigoAutorizacao("AUTH001")
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        EstornoResponseDTO resultado = estornoService.criarEstorno(requestDTO);

        // Assert
        assertThat(resultado.getTransacao().getDescricao().getStatus()).isEqualTo(StatusEstorno.CANCELADO.name());
        assertThat(resultado.getTransacao().getDescricao().getNsu()).isEqualTo("NSU123456");
        assertThat(resultado.getTransacao().getDescricao().getCodigoAutorizacao()).isEqualTo("AUTH001");

        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
    }

    // ========== TESTE 8: MAPEAMENTO NEGADO → NEGADO ==========

    @Test
    @DisplayName("8. Deve mapear NEGADO quando adquirente nega estorno")
    void deveMapeiarNegadoParaNegado() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(false);
        when(mapper.paraEntidade(any(EstornoRequestDTO.class)))
            .thenReturn(estorno);
        when(repository.save(any(Estorno.class))).thenAnswer(invocation -> {
            Estorno e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoNegada);

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.NEGADO.name())
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        EstornoResponseDTO resultado = estornoService.criarEstorno(requestDTO);

        // Assert
        assertThat(resultado.getTransacao().getDescricao().getStatus()).isEqualTo(StatusEstorno.NEGADO.name());

        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
    }

    // ========== TESTE 9: MAPEAMENTO PENDENTE (Circuit Breaker) ==========

    @Test
    @DisplayName("9. Deve manter PENDENTE quando Circuit Breaker ativa ou timeout")
    void deveMapeiarPendenteParaPendente() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(false);
        when(mapper.paraEntidade(any(EstornoRequestDTO.class)))
            .thenReturn(estorno);
        when(repository.save(any(Estorno.class))).thenAnswer(invocation -> {
            Estorno e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoPendente);

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.PENDENTE.name())
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        EstornoResponseDTO resultado = estornoService.criarEstorno(requestDTO);

        // Assert
        assertThat(resultado.getTransacao().getDescricao().getStatus()).isEqualTo(StatusEstorno.PENDENTE.name());

        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
    }

    // ========== TESTE 10: PUBLICAÇÃO DE EVENTOS ==========

    @Test
    @DisplayName("10. Deve publicar eventos EstornoCriado e EstornoStatusAlterado")
    void devePublicarEventos() {
        // Arrange
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));
        when(repository.existsEstornoCanceladoByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(false);
        when(mapper.paraEntidade(any(EstornoRequestDTO.class)))
            .thenReturn(estorno);
        when(repository.save(any(Estorno.class))).thenAnswer(invocation -> {
            Estorno e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada);

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        estornoService.criarEstorno(requestDTO);

        // Assert
        verify(eventoPublisher).publicarEstornoCriado(any(EstornoCriadoEvento.class));
        verify(eventoPublisher).publicarEstornoStatusAlterado(any(EstornoStatusAlteradoEvento.class));
        // Verifica que 2 eventos foram publicados (EstornoCriadoEvento + EstornoStatusAlteradoEvento)
        verify(eventPublisher, times(2)).publishEvent(any(Object.class));
    }

    // ========== TESTE 11: BUSCAR POR ID - SUCESSO ==========

    @Test
    @DisplayName("11. Deve buscar estorno por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        // Arrange
        Estorno estornoExistente = new Estorno();
        estornoExistente.setIdEstorno("EST-123-TEST");
        estornoExistente.setIdTransacao("TXN-123-TEST");
        estornoExistente.setStatus(StatusEstorno.CANCELADO);

        when(repository.findByIdEstorno("EST-123-TEST"))
            .thenReturn(Optional.of(estornoExistente));
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        EstornoResponseDTO responseDTO = EstornoResponseDTO.builder()
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(responseDTO);

        // Act
        EstornoResponseDTO resultado = estornoService.buscarPorIdEstorno("EST-123-TEST");

        // Assert
        assertThat(resultado).isNotNull();

        verify(repository).findByIdEstorno("EST-123-TEST");
    }

    // ========== TESTE 12: BUSCAR POR ID - NÃO ENCONTRADO ==========

    @Test
    @DisplayName("12. Deve lançar RecursoNaoEncontradoException quando estorno não existe")
    void deveLancarExcecaoQuandoEstornoNaoEncontrado() {
        // Arrange
        when(repository.findByIdEstorno("EST-999-INEXISTENTE"))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> estornoService.buscarPorIdEstorno("EST-999-INEXISTENTE"))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessageContaining("Estorno");

        verify(repository).findByIdEstorno("EST-999-INEXISTENTE");
    }

    // ========== TESTE 13: LISTAR POR ID TRANSAÇÃO ==========

    @Test
    @DisplayName("13. Deve listar estornos de um pagamento específico")
    void deveListarPorIdTransacao() {
        // Arrange
        Estorno estorno1 = new Estorno();
        estorno1.setIdEstorno("EST-001");
        estorno1.setIdTransacao("TXN-123-TEST");

        when(repository.findByIdTransacaoPagamento("TXN-123-TEST"))
            .thenReturn(Arrays.asList(estorno1));
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        EstornoResponseDTO dto1 = EstornoResponseDTO.builder()
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(dto1);

        // Act
        List<EstornoResponseDTO> resultados = estornoService.listarPorIdTransacao("TXN-123-TEST");

        // Assert
        assertThat(resultados).hasSize(1);

        verify(repository).findByIdTransacaoPagamento("TXN-123-TEST");
    }

    // ========== TESTE 14: LISTAR TODOS ==========

    @Test
    @DisplayName("14. Deve listar todos os estornos")
    void deveListarTodos() {
        // Arrange
        Estorno estorno1 = new Estorno();
        estorno1.setIdTransacao("TXN-001");
        Estorno estorno2 = new Estorno();
        estorno2.setIdTransacao("TXN-002");

        when(repository.findUltimosEstornos()).thenReturn(Arrays.asList(estorno1, estorno2));
        when(pagamentoRepository.findByIdTransacao("TXN-001")).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.findByIdTransacao("TXN-002")).thenReturn(Optional.of(pagamento));

        EstornoResponseDTO dto1 = EstornoResponseDTO.builder().build();
        EstornoResponseDTO dto2 = EstornoResponseDTO.builder().build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(dto1, dto2);

        // Act
        List<EstornoResponseDTO> resultados = estornoService.listarEstornos();

        // Assert
        assertThat(resultados).hasSize(2);

        verify(repository).findUltimosEstornos();
    }

    // ========== TESTE 15: LISTAR POR STATUS ==========

    @Test
    @DisplayName("15. Deve listar estornos por status")
    void deveListarPorStatus() {
        // Arrange
        Estorno estorno1 = new Estorno();
        estorno1.setIdTransacao("TXN-123-TEST");
        estorno1.setStatus(StatusEstorno.CANCELADO);

        when(repository.findByStatusOrderByCriadoEmDesc(StatusEstorno.CANCELADO))
            .thenReturn(Arrays.asList(estorno1));
        when(pagamentoRepository.findByIdTransacao("TXN-123-TEST"))
            .thenReturn(Optional.of(pagamento));

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .descricao(descricao)
            .build();
        EstornoResponseDTO dto1 = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
        when(mapper.paraDTO(any(Estorno.class), any(Pagamento.class))).thenReturn(dto1);

        // Act
        List<EstornoResponseDTO> resultados = estornoService.listarPorStatus(StatusEstorno.CANCELADO);

        // Assert
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getTransacao().getDescricao().getStatus()).isEqualTo(StatusEstorno.CANCELADO.name());

        verify(repository).findByStatusOrderByCriadoEmDesc(StatusEstorno.CANCELADO);
    }

    // ==================== TESTES DE REPROCESSAMENTO (ITEM 8) ====================

    @Test
    @DisplayName("16. Deve reprocessar estorno PENDENTE com sucesso → CANCELADO")
    void deveReprocessarEstornoPendenteComSucesso() {
        // Arrange
        when(reprocessamentoProperties.getMaxTentativas()).thenReturn(3);
        
        Estorno estornoPendente = new Estorno();
        estornoPendente.setId(1L);
        estornoPendente.setIdEstorno("EST-001");
        estornoPendente.setIdTransacao("TXN-123");
        estornoPendente.setStatus(StatusEstorno.PENDENTE);
        estornoPendente.setTentativasReprocessamento(0);
        estornoPendente.setValor(new BigDecimal("100.00"));

        when(repository.findEstornosPendentes())
            .thenReturn(Arrays.asList(estornoPendente));
        when(pagamentoRepository.findByIdTransacao("TXN-123"))
            .thenReturn(Optional.of(pagamento));
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoAprovada);
        when(repository.save(any(Estorno.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        estornoService.reprocessarEstornosPendentes();

        // Assert
        verify(repository).findEstornosPendentes();
        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
        verify(repository).save(argThat(e -> 
            e.getStatus() == StatusEstorno.CANCELADO &&
            e.getTentativasReprocessamento() == 1
        ));
        verify(eventoPublisher).publicarEstornoStatusAlterado(any());
    }

    @Test
    @DisplayName("17. Deve reprocessar estorno PENDENTE com falha → PENDENTE (incrementa tentativas)")
    void deveReprocessarEstornoPendenteComFalha() {
        // Arrange
        when(reprocessamentoProperties.getMaxTentativas()).thenReturn(3);
        
        Estorno estornoPendente = new Estorno();
        estornoPendente.setId(1L);
        estornoPendente.setIdEstorno("EST-002");
        estornoPendente.setIdTransacao("TXN-456");
        estornoPendente.setStatus(StatusEstorno.PENDENTE);
        estornoPendente.setTentativasReprocessamento(1); // Já tentou 1x
        estornoPendente.setValor(new BigDecimal("200.00"));

        when(repository.findEstornosPendentes())
            .thenReturn(Arrays.asList(estornoPendente));
        when(pagamentoRepository.findByIdTransacao("TXN-456"))
            .thenReturn(Optional.of(pagamento));
        when(adquirenteService.processarEstorno(any(AutorizacaoRequest.class)))
            .thenReturn(autorizacaoPendente); // Ainda pendente (Circuit Breaker OPEN)
        when(repository.save(any(Estorno.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        estornoService.reprocessarEstornosPendentes();

        // Assert
        verify(repository).findEstornosPendentes();
        verify(adquirenteService).processarEstorno(any(AutorizacaoRequest.class));
        verify(repository).save(argThat(e -> 
            e.getStatus() == StatusEstorno.PENDENTE &&
            e.getTentativasReprocessamento() == 2 // Incrementou de 1 para 2
        ));
        verify(eventoPublisher, never()).publicarEstornoStatusAlterado(any()); // Status não mudou
    }

    @Test
    @DisplayName("18. Não deve reprocessar estorno com max tentativas atingidas (DLQ)")
    void naoDeveReprocessarEstornoComMaxTentativas() {
        // Arrange
        when(reprocessamentoProperties.getMaxTentativas()).thenReturn(3);
        
        Estorno estornoDLQ = new Estorno();
        estornoDLQ.setId(1L);
        estornoDLQ.setIdEstorno("EST-DLQ");
        estornoDLQ.setIdTransacao("TXN-DLQ");
        estornoDLQ.setStatus(StatusEstorno.PENDENTE);
        estornoDLQ.setTentativasReprocessamento(3); // MAX = 3
        estornoDLQ.setValor(new BigDecimal("50.00"));

        when(repository.findEstornosPendentes())
            .thenReturn(Arrays.asList(estornoDLQ));

        // Act
        estornoService.reprocessarEstornosPendentes();

        // Assert
        verify(repository).findEstornosPendentes();
        verify(adquirenteService, never()).processarEstorno(any()); // NÃO tentou reprocessar
        verify(repository, never()).save(any()); // NÃO salvou
        verify(eventoPublisher, never()).publicarEstornoStatusAlterado(any());
    }

    @Test
    @DisplayName("19. Deve retornar sem erro quando batch está vazio")
    void deveRetornarSemErroQuandoBatchVazio() {
        // Arrange
        when(repository.findEstornosPendentes())
            .thenReturn(Arrays.asList()); // Lista vazia

        // Act
        estornoService.reprocessarEstornosPendentes();

        // Assert
        verify(repository).findEstornosPendentes();
        verify(adquirenteService, never()).processarEstorno(any());
        verify(repository, never()).save(any());
        verify(eventoPublisher, never()).publicarEstornoStatusAlterado(any());
    }

    @Test
    @DisplayName("20. Deve incrementar métrica DLQ quando estorno atinge máximo de tentativas")
    void deveIncrementarMetricaDLQQuandoEstornoAtingeMaximoTentativas() {
        // Arrange - Configurar mock do counter DLQ (apenas neste teste)
        when(meterRegistry.counter("reprocessamento.dlq.total", "tipo", "estorno"))
            .thenReturn(dlqCounter);
        
        // Arrange - Estorno que já atingiu max tentativas
        Estorno estornoDLQ = new Estorno();
        estornoDLQ.setId(99L);
        estornoDLQ.setIdEstorno("EST-DLQ-METRICS");
        estornoDLQ.setIdTransacao("TXN-DLQ-METRICS");
        estornoDLQ.setStatus(StatusEstorno.PENDENTE);
        estornoDLQ.setTentativasReprocessamento(3); // MAX = 3
        estornoDLQ.setValor(new BigDecimal("999.99"));

        when(repository.findEstornosPendentes())
            .thenReturn(Arrays.asList(estornoDLQ));

        // Act
        estornoService.reprocessarEstornosPendentes();

        // Assert - Verificar que counter DLQ foi incrementado
        verify(meterRegistry).counter("reprocessamento.dlq.total", "tipo", "estorno");
        verify(dlqCounter).increment(); // Counter foi incrementado
        verify(repository).findEstornosPendentes();
        verify(adquirenteService, never()).processarEstorno(any()); // Não processou
        verify(repository, never()).save(any()); // Não salvou
    }
}
