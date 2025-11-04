package br.com.sicredi.toolschallenge.admin.controller;

import br.com.sicredi.toolschallenge.admin.dto.TokenResponseDTO;
import br.com.sicredi.toolschallenge.shared.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller para operações administrativas.
 * 
 * Endpoints públicos (sem autenticação) para facilitar testes.
 */
@RestController
@RequestMapping("/admin/tokens")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Operações administrativas - Geração de tokens JWT")
public class AdminController {

    private final JwtService jwtService;

    @Value("${jwt.expiration-ms:2592000000}")
    private long expirationMs;

    /**
     * Gera token JWT pré-configurado para aplicação específica.
     * 
     * Apps disponíveis:
     * - frontend: scopes = pagamentos:read, pagamentos:write
     * - mobile: scopes = pagamentos:read
     * - admin: scopes = pagamentos:read, pagamentos:write, estornos:read, estornos:write
     * 
     * @param appName Nome da aplicação
     * @return 200 OK com token, expiracao e scopes
     */
    @PostMapping("/{appName}")
    @Operation(
        summary = "Gerar token JWT para aplicação",
        description = "Gera token pré-configurado com scopes específicos. Apps: 'frontend' (pagamentos R/W), 'mobile' (pagamentos R), 'admin' (todos scopes)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token gerado com sucesso",
            content = @Content(schema = @Schema(implementation = TokenResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Nome de app inválido (use: frontend, mobile ou admin)"
        )
    })
    public ResponseEntity<TokenResponseDTO> gerarToken(
        @Parameter(description = "Nome da aplicação", example = "frontend")
        @PathVariable String appName
    ) {
        log.info("POST /admin/tokens/{} - Gerando token", appName);

        List<String> scopes = obterScopesPorApp(appName);
        
        if (scopes.isEmpty()) {
            log.warn("App inválido: {}", appName);
            return ResponseEntity.badRequest().build();
        }

        Map<String, Object> claims = Map.of("scopes", scopes);
        String token = jwtService.generateToken(claims, appName);

        long expirationSeconds = expirationMs / 1000;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationSeconds);

        TokenResponseDTO response = TokenResponseDTO.builder()
                .token(token)
                .appName(appName)
                .scopes(scopes)
                .expiresAt(expiresAt)
                .expirationSeconds(expirationSeconds)
                .build();

        log.info("Token gerado - App: {}, Scopes: {}, Expira em: {}", 
                 appName, scopes, expiresAt);

        return ResponseEntity.ok(response);
    }

    /**
     * Lista apps disponíveis e seus scopes.
     * 
     * @return 200 OK com lista de apps e scopes
     */
    @GetMapping("/apps")
    @Operation(
        summary = "Listar apps disponíveis",
        description = "Retorna lista de apps que podem gerar tokens e seus respectivos scopes"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Lista de apps retornada com sucesso"
    )
    public ResponseEntity<Map<String, List<String>>> listarApps() {
        log.info("GET /admin/tokens/apps - Listando apps disponíveis");

        Map<String, List<String>> apps = Map.of(
            "frontend", List.of("pagamentos:read", "pagamentos:write"),
            "mobile", List.of("pagamentos:read"),
            "admin", List.of("pagamentos:read", "pagamentos:write", "estornos:read", "estornos:write")
        );

        return ResponseEntity.ok(apps);
    }

    /**
     * Determina scopes baseado no nome do app.
     * 
     * @param appName Nome da aplicação
     * @return Lista de scopes ou lista vazia se app inválido
     */
    private List<String> obterScopesPorApp(String appName) {
        return switch (appName.toLowerCase()) {
            case "frontend" -> List.of("pagamentos:read", "pagamentos:write");
            case "mobile" -> List.of("pagamentos:read");
            case "admin" -> List.of("pagamentos:read", "pagamentos:write", "estornos:read", "estornos:write");
            default -> {
                log.warn("App desconhecido: {}", appName);
                yield List.of();
            }
        };
    }
}
