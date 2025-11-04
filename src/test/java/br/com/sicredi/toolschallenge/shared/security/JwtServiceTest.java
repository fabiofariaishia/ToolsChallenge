package br.com.sicredi.toolschallenge.shared.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do JwtService.
 * 
 * Testa:
 * - Geração de tokens válidos
 * - Validação de tokens corretos
 * - Rejeição de tokens expirados
 * - Rejeição de tokens inválidos
 * - Extração correta de scopes
 */
@DisplayName("Testes Unitários - JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET = "test-secret-key-with-minimum-256-bits-for-hs256-algorithm";
    private static final long TEST_EXPIRATION_MS = 30_000L; // 30 segundos
    private static final String TEST_ISSUER = "test-issuer";

    @BeforeEach
    void setup() {
        jwtService = new JwtService(TEST_SECRET, TEST_EXPIRATION_MS, TEST_ISSUER);
    }

    @Test
    @DisplayName("Deve gerar token válido com claims e subject")
    void deveGerarTokenValido() {
        // Arrange
        String subject = "frontend";
        List<String> scopes = List.of("pagamentos:read", "pagamentos:write");
        Map<String, Object> claims = Map.of("scopes", scopes);

        // Act
        String token = jwtService.generateToken(claims, subject);

        // Assert
        assertNotNull(token, "Token não deve ser nulo");
        assertFalse(token.isBlank(), "Token não deve estar vazio");
        assertTrue(token.startsWith("eyJ"), "Token JWT deve começar com 'eyJ'");
        assertTrue(token.split("\\.").length == 3, "Token JWT deve ter 3 partes (header.payload.signature)");
    }

    @Test
    @DisplayName("Deve validar token correto como válido")
    void deveValidarTokenCorreto() {
        // Arrange
        String subject = "mobile";
        Map<String, Object> claims = Map.of("scopes", List.of("pagamentos:read"));
        String token = jwtService.generateToken(claims, subject);

        // Act
        boolean isValid = jwtService.validateToken(token);

        // Assert
        assertTrue(isValid, "Token gerado deve ser válido");
    }

    @Test
    @DisplayName("Deve rejeitar token expirado")
    void deveLancarExceptionTokenExpirado() throws InterruptedException {
        // Arrange - criar JwtService com expiração de 1 segundo
        JwtService shortLivedService = new JwtService(TEST_SECRET, 1000L, TEST_ISSUER);
        String subject = "admin";
        Map<String, Object> claims = Map.of("scopes", List.of("pagamentos:write"));
        String token = shortLivedService.generateToken(claims, subject);

        // Aguardar token expirar
        Thread.sleep(1100); // 1.1 segundos

        // Act
        boolean isValid = shortLivedService.validateToken(token);

        // Assert
        assertFalse(isValid, "Token expirado deve ser inválido");
    }

    @Test
    @DisplayName("Deve rejeitar token com assinatura inválida")
    void deveLancarExceptionTokenInvalido() {
        // Arrange
        String tokenInvalido = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0In0.invalid-signature";

        // Act
        boolean isValid = jwtService.validateToken(tokenInvalido);

        // Assert
        assertFalse(isValid, "Token com assinatura inválida deve ser rejeitado");
    }

    @Test
    @DisplayName("Deve rejeitar token malformado")
    void deveRejeitarTokenMalformado() {
        // Arrange
        String tokenMalformado = "token-sem-formato-jwt";

        // Act
        boolean isValid = jwtService.validateToken(tokenMalformado);

        // Assert
        assertFalse(isValid, "Token malformado deve ser rejeitado");
    }

    @Test
    @DisplayName("Deve rejeitar token nulo")
    void deveRejeitarTokenNulo() {
        // Act
        boolean isValid = jwtService.validateToken(null);

        // Assert
        assertFalse(isValid, "Token nulo deve ser rejeitado");
    }

    @Test
    @DisplayName("Deve rejeitar token vazio")
    void deveRejeitarTokenVazio() {
        // Act
        boolean isValid = jwtService.validateToken("");

        // Assert
        assertFalse(isValid, "Token vazio deve ser rejeitado");
    }

    @Test
    @DisplayName("Deve extrair scopes corretamente do token")
    void deveExtrairScopesCorretamente() {
        // Arrange
        String subject = "frontend";
        List<String> scopesEsperados = List.of("pagamentos:read", "pagamentos:write");
        Map<String, Object> claims = Map.of("scopes", scopesEsperados);
        String token = jwtService.generateToken(claims, subject);

        // Act
        List<String> scopesExtraidos = jwtService.extractScopes(token);

        // Assert
        assertNotNull(scopesExtraidos, "Scopes extraídos não devem ser nulos");
        assertEquals(2, scopesExtraidos.size(), "Deve extrair 2 scopes");
        assertTrue(scopesExtraidos.containsAll(scopesEsperados), "Deve conter todos os scopes esperados");
    }

    @Test
    @DisplayName("Deve extrair subject corretamente do token")
    void deveExtrairSubjectCorretamente() {
        // Arrange
        String subjectEsperado = "mobile";
        Map<String, Object> claims = Map.of("scopes", List.of("pagamentos:read"));
        String token = jwtService.generateToken(claims, subjectEsperado);

        // Act
        String subjectExtraido = jwtService.extractSubject(token);

        // Assert
        assertEquals(subjectEsperado, subjectExtraido, "Subject extraído deve ser igual ao esperado");
    }

    @Test
    @DisplayName("Deve extrair todos os claims do token")
    void deveExtrairClaimsCorretamente() {
        // Arrange
        String subject = "admin";
        List<String> scopes = List.of("pagamentos:read", "pagamentos:write", "estornos:read", "estornos:write");
        Map<String, Object> claims = Map.of("scopes", scopes);
        String token = jwtService.generateToken(claims, subject);

        // Act
        Claims claimsExtraidos = jwtService.extractClaims(token);

        // Assert
        assertNotNull(claimsExtraidos, "Claims não devem ser nulos");
        assertEquals(subject, claimsExtraidos.getSubject(), "Subject deve ser extraído corretamente");
        assertEquals(TEST_ISSUER, claimsExtraidos.getIssuer(), "Issuer deve ser extraído corretamente");
        assertNotNull(claimsExtraidos.getIssuedAt(), "IssuedAt não deve ser nulo");
        assertNotNull(claimsExtraidos.getExpiration(), "Expiration não deve ser nulo");
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando token não tem scopes")
    void deveRetornarListaVaziaQuandoSemScopes() {
        // Arrange
        String subject = "test";
        Map<String, Object> claims = Map.of(); // Sem scopes
        String token = jwtService.generateToken(claims, subject);

        // Act
        List<String> scopesExtraidos = jwtService.extractScopes(token);

        // Assert
        assertNotNull(scopesExtraidos, "Scopes devem retornar lista (não nulo)");
        assertTrue(scopesExtraidos.isEmpty(), "Scopes devem estar vazios");
    }
}
