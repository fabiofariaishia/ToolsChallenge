package br.com.sicredi.toolschallenge.pagamento.controller;

import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoRequestDTO;
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoResponseDTO;
import br.com.sicredi.toolschallenge.pagamento.service.PagamentoService;
import br.com.sicredi.toolschallenge.infra.idempotencia.annotation.Idempotente;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Controller REST para operações de Pagamento.
 * 
 * Endpoints:
 * - POST   /pagamentos          - Criar novo pagamento
 * - GET    /pagamentos/{id}     - Consultar pagamento por ID
 * - GET    /pagamentos          - Listar todos os pagamentos
 * - GET    /pagamentos/status/{status} - Listar por status
 * 
 * Todas as respostas seguem padrão REST com códigos HTTP apropriados.
 */
@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pagamentos", description = "Operações relacionadas a pagamentos com cartão")
public class PagamentoController {

    private final PagamentoService service;

    /**
     * Cria um novo pagamento.
     * 
     * @param request DTO validado com dados do pagamento
     * @return 201 Created com DTO de resposta e Location header
     */
    @PostMapping
    @Idempotente(ttl = 24, unidadeTempo = TimeUnit.HOURS)
    @PreAuthorize("hasAuthority('pagamentos:write')")
    @Operation(
        summary = "Criar novo pagamento",
        description = "Cria uma nova transação de pagamento e retorna o resultado da autorização. Requer header 'Chave-Idempotencia' para evitar duplicação."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Pagamento criado e processado com sucesso",
            content = @Content(schema = @Schema(implementation = PagamentoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos na requisição ou header 'Chave-Idempotencia' ausente"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor"
        )
    })
    public ResponseEntity<PagamentoResponseDTO> criarPagamento(
        @Parameter(description = "Chave de idempotência para evitar duplicatas", required = true)
        @RequestHeader(value = "Chave-Idempotencia", required = true) String chaveIdempotencia,
        @Valid @RequestBody PagamentoRequestDTO request
    ) {
        log.info("POST /pagamentos - Criando novo pagamento");
        
        PagamentoResponseDTO response = service.criarPagamento(request);
        
        log.info("Pagamento criado: {} - Status: {}", 
            response.getIdTransacao(), response.getStatus());
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Busca pagamento por ID de transação.
     * 
     * @param idTransacao UUID da transação
     * @return 200 OK com DTO de resposta ou 404 Not Found
     */
    @GetMapping("/{idTransacao}")
    @PreAuthorize("hasAuthority('pagamentos:read')")
    @Operation(
        summary = "Consultar pagamento por ID",
        description = "Busca um pagamento específico pelo seu ID de transação"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Pagamento encontrado",
            content = @Content(schema = @Schema(implementation = PagamentoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Pagamento não encontrado"
        )
    })
    public ResponseEntity<PagamentoResponseDTO> buscarPagamento(
        @Parameter(description = "ID da transação (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable String idTransacao
    ) {
        log.info("GET /pagamentos/{} - Buscando pagamento", idTransacao);
        
        PagamentoResponseDTO response = service.buscarPorIdTransacao(idTransacao);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos os pagamentos (últimos 100).
     * 
     * @return 200 OK com lista de DTOs
     */
    @GetMapping
    @PreAuthorize("hasAuthority('pagamentos:read')")
    @Operation(
        summary = "Listar todos os pagamentos",
        description = "Retorna os últimos 100 pagamentos ordenados por data de criação (mais recentes primeiro)"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista de pagamentos retornada com sucesso"
    )
    public ResponseEntity<List<PagamentoResponseDTO>> listarPagamentos() {
        log.info("GET /pagamentos - Listando todos os pagamentos");
        
        List<PagamentoResponseDTO> pagamentos = service.listarPagamentos();
        
        log.info("Retornando {} pagamentos", pagamentos.size());
        
        return ResponseEntity.ok(pagamentos);
    }

    /**
     * Lista pagamentos por status.
     * 
     * @param status Status do pagamento (PENDENTE, AUTORIZADO, NEGADO)
     * @return 200 OK com lista de DTOs
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('pagamentos:read')")
    @Operation(
        summary = "Listar pagamentos por status",
        description = "Retorna pagamentos filtrados por status específico"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista de pagamentos filtrada retornada com sucesso"
    )
    public ResponseEntity<List<PagamentoResponseDTO>> listarPorStatus(
        @Parameter(description = "Status do pagamento", example = "AUTORIZADO")
        @PathVariable StatusPagamento status
    ) {
        log.info("GET /pagamentos/status/{} - Listando por status", status);
        
        List<PagamentoResponseDTO> pagamentos = service.listarPorStatus(status);
        
        log.info("Retornando {} pagamentos com status {}", pagamentos.size(), status);
        
        return ResponseEntity.ok(pagamentos);
    }
}
