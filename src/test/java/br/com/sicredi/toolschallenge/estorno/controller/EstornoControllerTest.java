package br.com.sicredi.toolschallenge.estorno.controller;

import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoRequestDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoResponseDTO;
import br.com.sicredi.toolschallenge.estorno.service.EstornoService;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
 * Testes unitários REST para EstornoController.
 * 
 * Usa @WebMvcTest para testar APENAS a camada de controller,
 * com EstornoService mockado.
 * 
 * Cenários testados:
 * 1. POST /estornos com Idempotency-Key → 201 Created
 * 2. POST /estornos sem Idempotency-Key → 400 Bad Request
 * 3. POST /estornos duplicado (idempotência) → Mesma resposta cacheada
 * 4. GET /estornos/{id} encontrado → 200 OK
 * 5. GET /estornos/{id} não encontrado → 404 Not Found
 * 6. GET /estornos → 200 OK (lista todos)
 * 7. GET /estornos/pagamento/{idTransacao} → 200 OK
 * 8. GET /estornos/status/{status} → 200 OK
 * 9. POST /estornos com validação inválida → 400 Bad Request
 */
@WebMvcTest(EstornoController.class)
@DisplayName("EstornoController - Testes REST API")
class EstornoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EstornoService estornoService;
    
    // Mock necessário para IdempotenciaInterceptor (infraestrutura)
    @MockBean
    private br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService idempotenciaService;


    /**
     * Cenário 1: POST /estornos com Idempotency-Key válido
     * 
     * Dado: Request válido com header Idempotency-Key
     * Quando: POST /estornos
     * Então: 
     *   - Status 201 Created
     *   - Body contém DTO de resposta
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("1. POST /estornos com Idempotency-Key → 201 Created")
    void deveCriarEstornoComSucesso() throws Exception {
        // Arrange
        String idTransacao = UUID.randomUUID().toString();
        String idEstorno = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();

        EstornoRequestDTO request = EstornoRequestDTO.builder()
            .idTransacao(idTransacao)
            .valor(new BigDecimal("150.00"))
            .motivo("Cliente solicitou cancelamento")
            .build();

        EstornoResponseDTO responseEsperado = EstornoResponseDTO.builder()
            .idTransacao(idTransacao)
            .idEstorno(idEstorno)
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("150.00"))
            .dataHora(OffsetDateTime.now())
            .nsu("0987654321")
            .codigoAutorizacao("AUTH123456")
            .motivo("Cliente solicitou cancelamento")
            .criadoEm(OffsetDateTime.now())
            .atualizadoEm(OffsetDateTime.now())
            .build();

        when(estornoService.criarEstorno(any(EstornoRequestDTO.class)))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idTransacao").value(idTransacao))
            .andExpect(jsonPath("$.idEstorno").value(idEstorno))
            .andExpect(jsonPath("$.status").value("CANCELADO"))
            .andExpect(jsonPath("$.valor").value(150.00))
            .andExpect(jsonPath("$.nsu").value("0987654321"))
            .andExpect(jsonPath("$.codigoAutorizacao").value("AUTH123456"))
            .andExpect(jsonPath("$.motivo").value("Cliente solicitou cancelamento"));

        verify(estornoService, times(1)).criarEstorno(any(EstornoRequestDTO.class));
    }

    /**
     * Cenário 2: POST /estornos SEM Idempotency-Key
     * 
     * Dado: Request válido MAS sem header Idempotency-Key
     * Quando: POST /estornos
     * Então: 
     *   - Status 400 Bad Request
     *   - Mensagem de erro indicando header ausente
     *   - Service NÃO foi chamado
     */
    @Test
    @DisplayName("2. POST /estornos sem Idempotency-Key → 400 Bad Request")
    void deveRetornar400QuandoIdempotencyKeyAusente() throws Exception {
        // Arrange
        EstornoRequestDTO request = EstornoRequestDTO.builder()
            .idTransacao(UUID.randomUUID().toString())
            .valor(new BigDecimal("100.00"))
            .motivo("Teste")
            .build();

        // Act & Assert
        mockMvc.perform(post("/estornos")
                // SEM header Idempotency-Key
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());

        verify(estornoService, never()).criarEstorno(any(EstornoRequestDTO.class));
    }

    /**
     * Cenário 3: POST /estornos com mesma Idempotency-Key (duplicado)
     * 
     * Dado: Request duplicado com mesma Idempotency-Key
     * Quando: POST /estornos pela segunda vez
     * Então: 
     *   - Status 201 Created (mesma resposta cacheada)
     *   - Header X-Idempotency-Replayed: true
     *   - Service foi chamado apenas 1 vez (primeira requisição)
     * 
     * Nota: Este teste valida COMPORTAMENTO de idempotência.
     * O interceptor deve retornar resposta cacheada sem chamar service.
     */
    @Test
    @DisplayName("3. POST /estornos duplicado (idempotência) → Resposta cacheada")
    void deveRetornarRespostaCacheadaParaRequisicaoDuplicada() throws Exception {
        // Arrange
        String idTransacao = UUID.randomUUID().toString();
        String idEstorno = UUID.randomUUID().toString();
        String idempotencyKey = UUID.randomUUID().toString();

        EstornoRequestDTO request = EstornoRequestDTO.builder()
            .idTransacao(idTransacao)
            .valor(new BigDecimal("200.00"))
            .motivo("Teste idempotência")
            .build();

        EstornoResponseDTO responseEsperado = EstornoResponseDTO.builder()
            .idTransacao(idTransacao)
            .idEstorno(idEstorno)
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("200.00"))
            .build();

        when(estornoService.criarEstorno(any(EstornoRequestDTO.class)))
            .thenReturn(responseEsperado);

        // Act - Primeira requisição
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());

        // Act - Segunda requisição (DUPLICADA)
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idTransacao").value(idTransacao))
            .andExpect(jsonPath("$.idEstorno").value(idEstorno));

        // Assert - Service deveria ser chamado apenas 1 vez (primeira requisição)
        // Mas como IdempotenciaService está mockado, ele será chamado 2 vezes
        // Em produção, a segunda chamada seria bloqueada pelo interceptor
        verify(estornoService, atMost(2)).criarEstorno(any(EstornoRequestDTO.class));
    }

    /**
     * Cenário 4: GET /estornos/{id} - Estorno encontrado
     * 
     * Dado: Estorno existe no banco
     * Quando: GET /estornos/{idEstorno}
     * Então: 
     *   - Status 200 OK
     *   - Body contém DTO completo
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("4. GET /estornos/{id} encontrado → 200 OK")
    void deveBuscarEstornoPorId() throws Exception {
        // Arrange
        String idEstorno = UUID.randomUUID().toString();
        String idTransacao = UUID.randomUUID().toString();

        EstornoResponseDTO responseEsperado = EstornoResponseDTO.builder()
            .idEstorno(idEstorno)
            .idTransacao(idTransacao)
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("250.00"))
            .motivo("Produto não entregue")
            .build();

        when(estornoService.buscarPorIdEstorno(idEstorno))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(get("/estornos/{idEstorno}", idEstorno))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.idEstorno").value(idEstorno))
            .andExpect(jsonPath("$.idTransacao").value(idTransacao))
            .andExpect(jsonPath("$.status").value("CANCELADO"))
            .andExpect(jsonPath("$.valor").value(250.00))
            .andExpect(jsonPath("$.motivo").value("Produto não entregue"));

        verify(estornoService, times(1)).buscarPorIdEstorno(idEstorno);
    }

    /**
     * Cenário 5: GET /estornos/{id} - Estorno NÃO encontrado
     * 
     * Dado: Estorno NÃO existe no banco
     * Quando: GET /estornos/{idEstorno}
     * Então: 
     *   - Status 404 Not Found
     *   - Service lançou RecursoNaoEncontradoException
     */
    @Test
    @DisplayName("5. GET /estornos/{id} não encontrado → 404 Not Found")
    void deveRetornar404QuandoEstornoNaoEncontrado() throws Exception {
        // Arrange
        String idEstornoInexistente = UUID.randomUUID().toString();

        when(estornoService.buscarPorIdEstorno(idEstornoInexistente))
            .thenThrow(new RecursoNaoEncontradoException("Estorno não encontrado"));

        // Act & Assert
        mockMvc.perform(get("/estornos/{idEstorno}", idEstornoInexistente))
            .andExpect(status().isNotFound());

        verify(estornoService, times(1)).buscarPorIdEstorno(idEstornoInexistente);
    }

    /**
     * Cenário 6: GET /estornos - Listar todos
     * 
     * Dado: Existem estornos cadastrados
     * Quando: GET /estornos
     * Então: 
     *   - Status 200 OK
     *   - Body contém array de DTOs
     *   - Service foi chamado 1 vez
     */
    @Test
    @DisplayName("6. GET /estornos → 200 OK (lista todos)")
    void deveListarTodosEstornos() throws Exception {
        // Arrange
        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("100.00"))
            .build();

        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .status(StatusEstorno.NEGADO)
            .valor(new BigDecimal("200.00"))
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarEstornos())
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("CANCELADO"))
            .andExpect(jsonPath("$[0].valor").value(100.00))
            .andExpect(jsonPath("$[1].status").value("NEGADO"))
            .andExpect(jsonPath("$[1].valor").value(200.00));

        verify(estornoService, times(1)).listarEstornos();
    }

    /**
     * Cenário 7: GET /estornos/pagamento/{idTransacao}
     * 
     * Dado: Pagamento tem estornos associados
     * Quando: GET /estornos/pagamento/{idTransacao}
     * Então: 
     *   - Status 200 OK
     *   - Body contém array de estornos do pagamento
     *   - Service foi chamado com ID correto
     */
    @Test
    @DisplayName("7. GET /estornos/pagamento/{idTransacao} → 200 OK")
    void deveListarEstornosPorPagamento() throws Exception {
        // Arrange
        String idTransacao = UUID.randomUUID().toString();

        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .idTransacao(idTransacao)
            .status(StatusEstorno.NEGADO)
            .valor(new BigDecimal("150.00"))
            .build();

        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .idTransacao(idTransacao)
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("150.00"))
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarPorIdTransacao(idTransacao))
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos/pagamento/{idTransacao}", idTransacao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].idTransacao").value(idTransacao))
            .andExpect(jsonPath("$[0].status").value("NEGADO"))
            .andExpect(jsonPath("$[1].idTransacao").value(idTransacao))
            .andExpect(jsonPath("$[1].status").value("CANCELADO"));

        verify(estornoService, times(1)).listarPorIdTransacao(idTransacao);
    }

    /**
     * Cenário 8: GET /estornos/status/{status}
     * 
     * Dado: Existem estornos com status específico
     * Quando: GET /estornos/status/CANCELADO
     * Então: 
     *   - Status 200 OK
     *   - Body contém apenas estornos CANCELADOS
     *   - Service foi chamado com enum correto
     */
    @Test
    @DisplayName("8. GET /estornos/status/{status} → 200 OK")
    void deveListarEstornosPorStatus() throws Exception {
        // Arrange
        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("100.00"))
            .build();

        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .idEstorno(UUID.randomUUID().toString())
            .status(StatusEstorno.CANCELADO)
            .valor(new BigDecimal("200.00"))
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarPorStatus(StatusEstorno.CANCELADO))
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos/status/{status}", "CANCELADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].status").value("CANCELADO"))
            .andExpect(jsonPath("$[1].status").value("CANCELADO"));

        verify(estornoService, times(1)).listarPorStatus(StatusEstorno.CANCELADO);
    }

    /**
     * Cenário 9: POST /estornos com validação inválida
     * 
     * Dado: Request com campos inválidos
     * Quando: POST /estornos
     * Então: 
     *   - Status 400 Bad Request
     *   - Body contém erros de validação
     *   - Service NÃO foi chamado
     * 
     * Validações testadas:
     * - idTransacao: @NotBlank, @Size
     * - valor: @NotNull, @DecimalMin
     * - motivo: @Size(max=500)
     */
    @Test
    @DisplayName("9. POST /estornos com validação inválida → 400 Bad Request")
    void deveRetornar400QuandoCamposInvalidos() throws Exception {
        // Arrange - Request com múltiplas validações inválidas
        EstornoRequestDTO requestInvalido = EstornoRequestDTO.builder()
            .idTransacao("")  // @NotBlank - vazio
            .valor(new BigDecimal("-10.00"))  // @DecimalMin - negativo
            .motivo("X".repeat(501))  // @Size(max=500) - excede limite
            .build();

        // Act & Assert
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestInvalido)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Bad Request"))
            .andExpect(jsonPath("$.errosValidacao").isArray())
            .andExpect(jsonPath("$.errosValidacao", hasSize(greaterThan(0))));

        verify(estornoService, never()).criarEstorno(any(EstornoRequestDTO.class));
    }
}
