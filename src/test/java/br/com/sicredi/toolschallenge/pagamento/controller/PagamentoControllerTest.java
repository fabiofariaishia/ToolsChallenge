package br.com.sicredi.toolschallenge.pagamento.controller;

import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoRequestDTO;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoResponseDTO;
import br.com.sicredi.toolschallenge.pagamento.service.PagamentoService;
import br.com.sicredi.toolschallenge.shared.exception.GlobalExceptionHandler;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes unitários REST para PagamentoController (Slice Web).
 * 
 * Configuração:
 * - @WebMvcTest: Testa APENAS a camada de controller (slice)
 * - @AutoConfigureMockMvc(addFilters=false): Desliga filtros HTTP
 * - @Import(GlobalExceptionHandler.class): Carrega @ControllerAdvice para tratar exceções
 * - @WithMockUser: Simula usuário autenticado com scopes necessários
 * - @MockBean: JwtService e JwtAuthenticationFilter são @Component, então precisam de mock
 * 
 * IMPORTANTE:
 * - MVP Simplificado: Apenas 5 testes essenciais
 * - Idempotência: Testes movidos para PagamentoIntegrationTest (requerem infraestrutura)
 * - Segurança: Testes de 401/403 estão em SecurityIntegrationTest
 * 
 * Cenários testados (5):
 * 1. POST /pagamentos com Idempotency-Key → 201 Created
 * 2. GET /pagamentos/{id} encontrado → 200 OK
 * 3. GET /pagamentos/{id} não encontrado → 404 Not Found
 * 4. GET /pagamentos → 200 OK (lista todos)
 * 5. POST /pagamentos com validação inválida → 400 Bad Request
 */
@WebMvcTest(controllers = PagamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(authorities = {"pagamentos:read", "pagamentos:write"})
@DisplayName("PagamentoController - Testes REST API")
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PagamentoService pagamentoService;

    // Mock necessário: IdempotenciaInterceptor está registrado no WebMvcConfigurer
    @MockBean
    private br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService idempotenciaService;

    // Mocks de Security necessários porque são @Component escaneados pelo Spring
    @MockBean
    private br.com.sicredi.toolschallenge.shared.security.JwtService jwtService;

    @MockBean
    private br.com.sicredi.toolschallenge.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Cenário 1: POST /pagamentos com Idempotency-Key válido
     * 
     * Dado: Request válido com header Idempotency-Key
     * Quando: POST /pagamentos
     * Então: 
     *   - Status 201 Created
     *   - Body contém DTO de resposta
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("1. POST /pagamentos com Idempotency-Key → 201 Created")
    void deveCriarPagamentoComSucesso() throws Exception {
        // Arrange
        String idTransacao = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();

        PagamentoRequestDTO request = PagamentoRequestDTO.builder()
            .estabelecimento("Loja X")
            .valor(new BigDecimal("150.00"))
            .moeda("BRL")
            .tipoPagamento(TipoPagamento.AVISTA)
            .parcelas(1)
            .cartaoMascarado("4111********1111")
            .build();

        PagamentoResponseDTO responseEsperado = PagamentoResponseDTO.builder()
            .idTransacao(idTransacao)
            .estabelecimento("Loja X")
            .valor(new BigDecimal("150.00"))
            .moeda("BRL")
            .tipoPagamento(TipoPagamento.AVISTA)
            .parcelas(1)
            .cartaoMascarado("4111********1111")
            .status(StatusPagamento.AUTORIZADO)
            .nsu("123456789")
            .codigoAutorizacao("AUTH987654")
            .dataHora(OffsetDateTime.now())
            .criadoEm(OffsetDateTime.now())
            .atualizadoEm(OffsetDateTime.now())
            .build();

        when(pagamentoService.criarPagamento(any(PagamentoRequestDTO.class)))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(post("/pagamentos")
                .header("Chave-Idempotencia", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idTransacao").value(idTransacao))
            .andExpect(jsonPath("$.estabelecimento").value("Loja X"))
            .andExpect(jsonPath("$.status").value("AUTORIZADO"))
            .andExpect(jsonPath("$.valor").value(150.00))
            .andExpect(jsonPath("$.nsu").value("123456789"))
            .andExpect(jsonPath("$.codigoAutorizacao").value("AUTH987654"));

        verify(pagamentoService, times(1)).criarPagamento(any(PagamentoRequestDTO.class));
    }

    /**
     * Cenário 2: GET /pagamentos/{id} - Pagamento encontrado
     * 
     * Dado: Pagamento existe no banco
     * Quando: GET /pagamentos/{idTransacao}
     * Então: 
     *   - Status 200 OK
     *   - Body contém DTO completo
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("2. GET /pagamentos/{id} encontrado → 200 OK")
    void deveBuscarPagamentoPorId() throws Exception {
        // Arrange
        String idTransacao = UUID.randomUUID().toString();

        PagamentoResponseDTO responseEsperado = PagamentoResponseDTO.builder()
            .idTransacao(idTransacao)
            .estabelecimento("Loja Y")
            .valor(new BigDecimal("250.00"))
            .tipoPagamento(TipoPagamento.PARCELADO_LOJA)
            .parcelas(3)
            .status(StatusPagamento.AUTORIZADO)
            .nsu("987654321")
            .codigoAutorizacao("AUTH123456")
            .build();

        when(pagamentoService.buscarPorIdTransacao(idTransacao))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(get("/pagamentos/{idTransacao}", idTransacao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idTransacao").value(idTransacao))
            .andExpect(jsonPath("$.estabelecimento").value("Loja Y"))
            .andExpect(jsonPath("$.status").value("AUTORIZADO"))
            .andExpect(jsonPath("$.valor").value(250.00))
            .andExpect(jsonPath("$.nsu").value("987654321"));

        verify(pagamentoService, times(1)).buscarPorIdTransacao(idTransacao);
    }

    /**
     * Cenário 3: GET /pagamentos/{id} - Pagamento NÃO encontrado
     * 
     * Dado: Pagamento NÃO existe no banco
     * Quando: GET /pagamentos/{idTransacao}
     * Então: 
     *   - Status 404 Not Found
     *   - Service lançou RecursoNaoEncontradoException
     */
    @Test
    @DisplayName("3. GET /pagamentos/{id} não encontrado → 404 Not Found")
    void deveRetornar404QuandoPagamentoNaoEncontrado() throws Exception {
        // Arrange
        String idTransacaoInexistente = UUID.randomUUID().toString();

        when(pagamentoService.buscarPorIdTransacao(idTransacaoInexistente))
            .thenThrow(new RecursoNaoEncontradoException("Pagamento não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/pagamentos/{idTransacao}", idTransacaoInexistente))
            .andExpect(status().isNotFound());

        verify(pagamentoService, times(1)).buscarPorIdTransacao(idTransacaoInexistente);
    }

    /**
     * Cenário 4: GET /pagamentos - Listar todos
     * 
     * Dado: Existem pagamentos cadastrados
     * Quando: GET /pagamentos
     * Então: 
     *   - Status 200 OK
     *   - Body contém array de DTOs
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("4. GET /pagamentos → 200 OK (lista todos)")
    void deveListarTodosPagamentos() throws Exception {
        // Arrange
        PagamentoResponseDTO pagamento1 = PagamentoResponseDTO.builder()
            .idTransacao(UUID.randomUUID().toString())
            .estabelecimento("Loja ABC")
            .status(StatusPagamento.AUTORIZADO)
            .valor(new BigDecimal("100.00"))
            .build();

        PagamentoResponseDTO pagamento2 = PagamentoResponseDTO.builder()
            .idTransacao(UUID.randomUUID().toString())
            .estabelecimento("Loja XYZ")
            .status(StatusPagamento.NEGADO)
            .valor(new BigDecimal("200.00"))
            .build();

        List<PagamentoResponseDTO> pagamentos = Arrays.asList(pagamento1, pagamento2);

        when(pagamentoService.listarPagamentos())
            .thenReturn(pagamentos);

        // Act & Assert
        mockMvc.perform(get("/pagamentos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].estabelecimento").value("Loja ABC"))
            .andExpect(jsonPath("$[0].status").value("AUTORIZADO"))
            .andExpect(jsonPath("$[0].valor").value(100.00))
            .andExpect(jsonPath("$[1].estabelecimento").value("Loja XYZ"))
            .andExpect(jsonPath("$[1].status").value("NEGADO"))
            .andExpect(jsonPath("$[1].valor").value(200.00));

        verify(pagamentoService, times(1)).listarPagamentos();
    }

    /**
     * Cenário 5: POST /pagamentos com validação inválida
     * 
     * Dado: Request com campos inválidos
     * Quando: POST /pagamentos
     * Então: 
     *   - Status 400 Bad Request
     *   - Body contém erros de validação
     *   - Service NÃO foi chamado
     * 
     * Validações testadas:
     * - descricao: @NotBlank, @Size
     * - valor: @NotNull, @DecimalMin
     * - tipoPagamento: @NotNull
     */
    @Test
    @DisplayName("5. POST /pagamentos com validação inválida → 400 Bad Request")
    void deveRetornar400QuandoCamposInvalidos() throws Exception {
        // Arrange - Request com múltiplas validações inválidas
        PagamentoRequestDTO requestInvalido = PagamentoRequestDTO.builder()
            .estabelecimento("")  // @NotBlank - vazio
            .valor(new BigDecimal("-10.00"))  // @DecimalMin - negativo
            .tipoPagamento(null)  // @NotNull
            .parcelas(null)  // @NotNull
            .cartaoMascarado("123")  // @Pattern - formato inválido
            .build();

        // Act & Assert
        mockMvc.perform(post("/pagamentos")
                .header("Chave-Idempotencia", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Bad Request"))
            .andExpect(jsonPath("$.errosValidacao").isArray())
            .andExpect(jsonPath("$.errosValidacao", hasSize(greaterThan(0))));

        verify(pagamentoService, never()).criarPagamento(any(PagamentoRequestDTO.class));
    }
}
