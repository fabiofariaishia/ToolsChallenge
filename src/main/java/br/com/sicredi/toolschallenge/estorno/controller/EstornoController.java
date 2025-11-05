package br.com.sicredi.toolschallenge.estorno.controller;

import br.com.sicredi.toolschallenge.estorno.domain.StatusEstorno;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoRequestDTO;
import br.com.sicredi.toolschallenge.estorno.dto.EstornoResponseDTO;
import br.com.sicredi.toolschallenge.estorno.service.EstornoService;
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
 * Controller REST para operações de Estorno.
 * 
 * Endpoints:
 * - POST   /estornos                      - Criar novo estorno
 * - GET    /estornos/{id}                 - Consultar estorno por ID
 * - GET    /estornos                      - Listar todos os estornos
 * - GET    /estornos/pagamento/{idTransacao} - Listar estornos de um pagamento
 * - GET    /estornos/status/{status}      - Listar por status
 */
@RestController
@RequestMapping("/estornos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Estornos", description = "Operações relacionadas a estornos de pagamentos")
public class EstornoController {

    private final EstornoService service;

    /**
     * Cria uma nova solicitação de estorno.
     * 
     * @param request DTO validado com dados do estorno
     * @return 201 Created com DTO de resposta
     */
    @PostMapping
    @Idempotente(ttl = 24, unidadeTempo = TimeUnit.HOURS)
    @PreAuthorize("hasAuthority('estornos:write')")
    @Operation(
        summary = "Criar novo estorno",
        description = "Solicita o estorno de um pagamento autorizado. Apenas estorno total é permitido, dentro de 24 horas. Requer header 'Chave-Idempotencia'."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Dados do estorno a ser processado",
        required = true,
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = EstornoRequestDTO.class),
            examples = {
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Estorno com motivo",
                    summary = "Exemplo de estorno com motivo detalhado",
                    description = "Estorno total de pagamento autorizado com justificativa",
                    value = """
                        {
                          "idTransacao": "123e4567-e89b-12d3-a456-426614174000",
                          "valor": 150.50,
                          "motivo": "Cliente solicitou cancelamento da compra"
                        }
                        """
                ),
                @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "Estorno sem motivo",
                    summary = "Exemplo de estorno sem motivo",
                    description = "Estorno mínimo (motivo é opcional)",
                    value = """
                        {
                          "idTransacao": "987fcdeb-51a2-43f6-b789-012345678901",
                          "valor": 300.00
                        }
                        """
                )
            }
        )
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Estorno criado e processado com sucesso",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = EstornoResponseDTO.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Estorno cancelado",
                        summary = "Estorno autorizado pelo adquirente",
                        description = "Exemplo de estorno processado com sucesso - status CANCELADO",
                        value = """
                            {
                              "transacao": {
                                "cartao": "4444********1234",
                                "id": "10002356890001",
                                "descricao": {
                                  "valor": 50.00,
                                  "dataHora": "01/05/2021 18:30:00",
                                  "estabelecimento": "PetShop Mundo cão",
                                  "nsu": "1234567890",
                                  "codigoAutorizacao": "147258369",
                                  "status": "CANCELADO"
                                },
                                "formaPagamento": {
                                  "tipo": "AVISTA",
                                  "parcelas": 1
                                }
                              }
                            }
                            """
                    ),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "Estorno negado",
                        summary = "Estorno rejeitado pelo adquirente",
                        description = "Exemplo de estorno negado - status NEGADO",
                        value = """
                            {
                              "transacao": {
                                "cartao": "5555********6789",
                                "id": "10002356890002",
                                "descricao": {
                                  "valor": 200.00,
                                  "dataHora": "02/05/2021 10:15:00",
                                  "estabelecimento": "Restaurante Bom Sabor",
                                  "nsu": null,
                                  "codigoAutorizacao": null,
                                  "status": "NEGADO"
                                },
                                "formaPagamento": {
                                  "tipo": "PARCELADO_LOJA",
                                  "parcelas": 3
                                }
                              }
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Requisição inválida: valor incorreto, pagamento não encontrado ou header 'Chave-Idempotencia' ausente",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado: token JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado: token válido mas sem permissão 'estornos:write'",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflito: já existe estorno para este pagamento ou pagamento não está autorizado",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        )
    })
    public ResponseEntity<EstornoResponseDTO> criarEstorno(
        @Parameter(description = "Chave de idempotência para evitar duplicatas", required = true)
        @RequestHeader(value = "Chave-Idempotencia", required = true) String chaveIdempotencia,
        @Valid @RequestBody EstornoRequestDTO request
    ) {
        log.info("POST /estornos - Criando estorno para pagamento: {}", request.getIdTransacao());

        EstornoResponseDTO response = service.criarEstorno(request);

        log.info("Estorno criado para transação: {} - Status: {}", 
            response.getTransacao().getId(), 
            response.getTransacao().getDescricao().getStatus());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    /**
     * Busca estorno por ID.
     * 
     * @param idEstorno UUID do estorno
     * @return 200 OK com DTO de resposta ou 404 Not Found
     */
    @GetMapping("/{idEstorno}")
    @PreAuthorize("hasAuthority('estornos:read')")
    @Operation(
        summary = "Consultar estorno por ID",
        description = "Busca um estorno específico pelo seu ID único"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Estorno encontrado",
            content = @Content(schema = @Schema(implementation = EstornoResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado: token JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado: token válido mas sem permissão 'estornos:read'",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Estorno não encontrado com o ID fornecido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        )
    })
    public ResponseEntity<EstornoResponseDTO> buscarEstorno(
        @Parameter(description = "ID do estorno (UUID)", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable String idEstorno
    ) {
        log.info("GET /estornos/{} - Buscando estorno", idEstorno);

        EstornoResponseDTO response = service.buscarPorIdEstorno(idEstorno);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista estornos de um pagamento específico.
     * 
     * @param idTransacao UUID da transação do pagamento
     * @return 200 OK com lista de DTOs
     */
    @GetMapping("/pagamento/{idTransacao}")
    @PreAuthorize("hasAuthority('estornos:read')")
    @Operation(
        summary = "Listar estornos de um pagamento",
        description = "Retorna todos os estornos (tentativas) relacionados a um pagamento específico"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de estornos retornada com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado: token JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado: token válido mas sem permissão 'estornos:read'",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        )
    })
    public ResponseEntity<List<EstornoResponseDTO>> listarEstornosPorPagamento(
        @Parameter(description = "ID da transação do pagamento", example = "123e4567-e89b-12d3-a456-426614174000")
        @PathVariable String idTransacao
    ) {
        log.info("GET /estornos/pagamento/{} - Listando estornos do pagamento", idTransacao);

        List<EstornoResponseDTO> estornos = service.listarPorIdTransacao(idTransacao);

        log.info("Retornando {} estornos para o pagamento {}", estornos.size(), idTransacao);

        return ResponseEntity.ok(estornos);
    }

    /**
     * Lista todos os estornos (últimos 100).
     * 
     * @return 200 OK com lista de DTOs
     */
    @GetMapping
    @PreAuthorize("hasAuthority('estornos:read')")
    @Operation(
        summary = "Listar todos os estornos",
        description = "Retorna os últimos 100 estornos ordenados por data de criação (mais recentes primeiro)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de estornos retornada com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado: token JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado: token válido mas sem permissão 'estornos:read'",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        )
    })
    public ResponseEntity<List<EstornoResponseDTO>> listarEstornos() {
        log.info("GET /estornos - Listando todos os estornos");

        List<EstornoResponseDTO> estornos = service.listarEstornos();

        log.info("Retornando {} estornos", estornos.size());

        return ResponseEntity.ok(estornos);
    }

    /**
     * Lista estornos por status.
     * 
     * @param status Status do estorno (PENDENTE, CANCELADO, NEGADO)
     * @return 200 OK com lista de DTOs
     */
    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('estornos:read')")
    @Operation(
        summary = "Listar estornos por status",
        description = "Retorna estornos filtrados por status específico"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lista de estornos filtrada retornada com sucesso"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Não autenticado: token JWT ausente ou inválido",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Acesso negado: token válido mas sem permissão 'estornos:read'",
            content = @Content(schema = @Schema(implementation = br.com.sicredi.toolschallenge.shared.exception.ErroResposta.class))
        )
    })
    public ResponseEntity<List<EstornoResponseDTO>> listarPorStatus(
        @Parameter(description = "Status do estorno", example = "CANCELADO")
        @PathVariable StatusEstorno status
    ) {
        log.info("GET /estornos/status/{} - Listando por status", status);

        List<EstornoResponseDTO> estornos = service.listarPorStatus(status);

        log.info("Retornando {} estornos com status {}", estornos.size(), status);

        return ResponseEntity.ok(estornos);
    }
}
