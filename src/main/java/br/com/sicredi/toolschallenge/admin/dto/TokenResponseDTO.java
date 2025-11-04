package br.com.sicredi.toolschallenge.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de resposta para geração de token JWT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response DTO contendo token JWT gerado e metadados")
public class TokenResponseDTO {

    @Schema(description = "Token JWT (Bearer)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Nome da aplicação", example = "frontend")
    private String appName;

    @Schema(description = "Scopes/permissões concedidas", example = "[\"pagamentos:read\", \"pagamentos:write\"]")
    private List<String> scopes;

    @Schema(description = "Data/hora de expiração do token", example = "2025-12-04T10:30:00")
    private LocalDateTime expiresAt;

    @Schema(description = "Tempo de expiração em segundos", example = "2592000")
    private Long expirationSeconds;
}
