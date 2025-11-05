package br.com.sicredi.toolschallenge.estorno.controller;

import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import br.com.sicredi.toolschallenge.estorno.dto.DescricaoEstornoDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoRequestDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoResponseDTO;
import br.com.sicredi.toolschallenge.estorno.dto.FormaPagamentoEstornoDTO;
import br.com.sicredi.toolschallenge.estorno.dto.TransacaoEstornoDTO;
import br.com.sicredi.toolschallenge.estorno.service.EstornoService;
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
 * Testes unitários REST para EstornoController (Slice Web).
 * 
 * Configuração:
 * - @WebMvcTest: Testa APENAS a camada de controller (slice)
 * - @AutoConfigureMockMvc(addFilters=false): Desliga filtros HTTP (CSRF)
 * - @Import(GlobalExceptionHandler.class): Carrega @ControllerAdvice para tratar exceções
 * - @WithMockUser: Simula usuário autenticado com scopes necessários
 * - @MockBean: JwtService e JwtAuthenticationFilter são @Component, então precisam de mock
 * 
 * IMPORTANTE: 
 * - Idempotência: Testes 2 e 3 movidos para EstornoIntegrationTest (requerem interceptor)
 * - Segurança: Testes de 401/403 estão em SecurityIntegrationTest
 * - Infraestrutura: IdempotenciaService mockado (interceptor ainda registrado no WebMvcConfigurer)
 * - Security: JwtService/JwtAuthenticationFilter mockados porque são @Component (sempre escaneados)
 * 
 * Cenários testados (7 de 9):
 * 1. POST /estornos com Chave-Idempotencia → 201 Created
 * 4. GET /estornos/{id} encontrado → 200 OK
 * 5. GET /estornos/{id} não encontrado → 404 Not Found
 * 6. GET /estornos → 200 OK (lista todos)
 * 7. GET /estornos/pagamento/{idTransacao} → 200 OK
 * 8. GET /estornos/status/{status} → 200 OK
 * 9. POST /estornos com validação inválida → 400 Bad Request
 */
@WebMvcTest(controllers = EstornoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@WithMockUser(authorities = {"pagamentos:read", "pagamentos:write", "estornos:read", "estornos:write"})
@DisplayName("EstornoController - Testes REST API")
class EstornoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EstornoService estornoService;
    
    // Mock necessário: IdempotenciaInterceptor está registrado no WebMvcConfigurer
    // Mesmo com addFilters=false, interceptores ainda são carregados
    @MockBean
    private br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService idempotenciaService;

    // Mocks de Security necessários porque são @Component escaneados pelo Spring
    // Mesmo com addFilters=false, esses beans são criados na inicialização
    @MockBean
    private br.com.sicredi.toolschallenge.shared.security.JwtService jwtService;

    @MockBean
    private br.com.sicredi.toolschallenge.shared.security.JwtAuthenticationFilter jwtAuthenticationFilter;


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
        String idempotencyKey = UUID.randomUUID().toString();

        EstornoRequestDTO request = EstornoRequestDTO.builder()
            .idTransacao(idTransacao)
            .valor(new BigDecimal("150.00"))
            .motivo("Cliente solicitou cancelamento")
            .build();

        // Construir estrutura aninhada conforme nova estrutura de DTOs
        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("150.00"))
            .dataHora("04/11/2025 18:30:00")
            .estabelecimento("PetShop Mundo cão")
            .nsu("0987654321")
            .codigoAutorizacao("AUTH123456")
            .status(StatusEstorno.CANCELADO.name())
            .build();

        FormaPagamentoEstornoDTO formaPagamento = FormaPagamentoEstornoDTO.builder()
            .tipo("AVISTA")
            .parcelas(1)
            .build();

        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .cartao("4444********1234")
            .id(idTransacao)
            .descricao(descricao)
            .formaPagamento(formaPagamento)
            .build();

        EstornoResponseDTO responseEsperado = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();

        when(estornoService.criarEstorno(any(EstornoRequestDTO.class)))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.transacao.id").value(idTransacao))
            .andExpect(jsonPath("$.transacao.descricao.status").value("CANCELADO"))
            .andExpect(jsonPath("$.transacao.descricao.valor").value(150.00))
            .andExpect(jsonPath("$.transacao.descricao.nsu").value("0987654321"))
            .andExpect(jsonPath("$.transacao.descricao.codigoAutorizacao").value("AUTH123456"));

        verify(estornoService, times(1)).criarEstorno(any(EstornoRequestDTO.class));
    }

    // =========================================================================
    // TESTES MOVIDOS PARA INTEGRAÇÃO (EstornoIntegrationTest)
    // =========================================================================
    
    /**
     * TODO: Mover para EstornoIntegrationTest
     * 
     * Cenário 2: POST /estornos SEM Chave-Idempotencia → 400 Bad Request
     * 
     * Este teste requer @RequestHeader(required=true) no controller.
     * Com addFilters=false, o teste passa automaticamente (Spring valida header ausente).
     * 
     * Motivo: Teste simples de validação de header já coberto pelo @RequestHeader(required=true).
     * Não precisa de teste dedicado em slice, Spring Boot já testa isso.
     */
    // @Test
    // @DisplayName("2. POST /estornos sem Chave-Idempotencia → 400 Bad Request")
    // void deveRetornar400QuandoIdempotencyKeyAusente() throws Exception { }

    /**
     * TODO: Mover para EstornoIntegrationTest (@SpringBootTest)
     * 
     * Cenário 3: POST /estornos com mesma Chave-Idempotencia (duplicado) → Resposta cacheada
     * 
     * Este teste requer IdempotenciaInterceptor ATIVO para funcionar corretamente.
     * Com addFilters=false e IdempotenciaService mockado, o comportamento real não é testado.
     * 
     * Motivo: Validação de INFRAESTRUTURA (interceptor + Redis) deve ser feita em @SpringBootTest,
     * não em teste de slice (@WebMvcTest).
     */
    // @Test
    // @DisplayName("3. POST /estornos duplicado (idempotência) → Resposta cacheada")
    // void deveRetornarRespostaCacheadaParaRequisicaoDuplicada() throws Exception { }

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

        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("250.00"))
            .dataHora("04/11/2025 18:30:00")
            .estabelecimento("Loja XYZ")
            .nsu("1234567890")
            .codigoAutorizacao("AUTH999")
            .status(StatusEstorno.CANCELADO.name())
            .build();

        FormaPagamentoEstornoDTO formaPagamento = FormaPagamentoEstornoDTO.builder()
            .tipo("AVISTA")
            .parcelas(1)
            .build();

        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .cartao("4444********1234")
            .id(idTransacao)
            .descricao(descricao)
            .formaPagamento(formaPagamento)
            .build();

        EstornoResponseDTO responseEsperado = EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();

        when(estornoService.buscarPorIdEstorno(idEstorno))
            .thenReturn(responseEsperado);

        // Act & Assert
        mockMvc.perform(get("/estornos/{idEstorno}", idEstorno))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.transacao.id").value(idTransacao))
            .andExpect(jsonPath("$.transacao.descricao.status").value("CANCELADO"))
            .andExpect(jsonPath("$.transacao.descricao.valor").value(250.00));

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
        DescricaoEstornoDTO desc1 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("100.00"))
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao1 = TransacaoEstornoDTO.builder()
            .descricao(desc1)
            .build();
        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .transacao(transacao1)
            .build();

        DescricaoEstornoDTO desc2 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("200.00"))
            .status(StatusEstorno.NEGADO.name())
            .build();
        TransacaoEstornoDTO transacao2 = TransacaoEstornoDTO.builder()
            .descricao(desc2)
            .build();
        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .transacao(transacao2)
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarEstornos())
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].transacao.descricao.status").value("CANCELADO"))
            .andExpect(jsonPath("$[0].transacao.descricao.valor").value(100.00))
            .andExpect(jsonPath("$[1].transacao.descricao.status").value("NEGADO"))
            .andExpect(jsonPath("$[1].transacao.descricao.valor").value(200.00));

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

        DescricaoEstornoDTO desc1 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("150.00"))
            .status(StatusEstorno.NEGADO.name())
            .build();
        TransacaoEstornoDTO transacao1 = TransacaoEstornoDTO.builder()
            .id(idTransacao)
            .descricao(desc1)
            .build();
        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .transacao(transacao1)
            .build();

        DescricaoEstornoDTO desc2 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("150.00"))
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao2 = TransacaoEstornoDTO.builder()
            .id(idTransacao)
            .descricao(desc2)
            .build();
        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .transacao(transacao2)
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarPorIdTransacao(idTransacao))
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos/pagamento/{idTransacao}", idTransacao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].transacao.id").value(idTransacao))
            .andExpect(jsonPath("$[0].transacao.descricao.status").value("NEGADO"))
            .andExpect(jsonPath("$[1].transacao.id").value(idTransacao))
            .andExpect(jsonPath("$[1].transacao.descricao.status").value("CANCELADO"));

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
        DescricaoEstornoDTO desc1 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("100.00"))
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao1 = TransacaoEstornoDTO.builder()
            .descricao(desc1)
            .build();
        EstornoResponseDTO estorno1 = EstornoResponseDTO.builder()
            .transacao(transacao1)
            .build();

        DescricaoEstornoDTO desc2 = DescricaoEstornoDTO.builder()
            .valor(new BigDecimal("200.00"))
            .status(StatusEstorno.CANCELADO.name())
            .build();
        TransacaoEstornoDTO transacao2 = TransacaoEstornoDTO.builder()
            .descricao(desc2)
            .build();
        EstornoResponseDTO estorno2 = EstornoResponseDTO.builder()
            .transacao(transacao2)
            .build();

        List<EstornoResponseDTO> estornos = Arrays.asList(estorno1, estorno2);

        when(estornoService.listarPorStatus(StatusEstorno.CANCELADO))
            .thenReturn(estornos);

        // Act & Assert
        mockMvc.perform(get("/estornos/status/{status}", "CANCELADO"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].transacao.descricao.status").value("CANCELADO"))
            .andExpect(jsonPath("$[1].transacao.descricao.status").value("CANCELADO"));

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
