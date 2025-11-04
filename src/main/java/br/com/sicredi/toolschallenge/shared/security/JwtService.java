package br.com.sicredi.toolschallenge.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service responsável por gerar e validar tokens JWT.
 * Utiliza algoritmo HS256 (HMAC SHA-256) com chave secreta configurável.
 */
@Service
@Slf4j
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:2592000000}") long expirationMs, // 30 dias default
            @Value("${jwt.issuer:toolschallenge-api}") String issuer) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
        log.info("JwtService inicializado - Algoritmo: HS256, Expiracao: {}ms ({}d)", 
                 expirationMs, expirationMs / 1000 / 60 / 60 / 24);
    }

    /**
     * Gera token JWT com claims customizados.
     *
     * @param claims Claims adicionais (ex: scopes, roles)
     * @param subject Subject do token (ex: appName)
     * @return Token JWT assinado
     */
    public String generateToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();

        log.debug("🔑 Token gerado - Subject: {}, Expira em: {}", subject, expiration);
        return token;
    }

    /**
     * Valida token JWT.
     *
     * @param token Token JWT
     * @return true se válido, false se inválido/expirado
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.warn("⚠️ Token com assinatura inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("⚠️ Token malformado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Token expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("⚠️ Token não suportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Token vazio ou nulo: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extrai todos os claims do token.
     *
     * @param token Token JWT
     * @return Claims do token
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrai subject (app name) do token.
     *
     * @param token Token JWT
     * @return Subject do token
     */
    public String extractSubject(String token) {
        return extractClaims(token).getSubject();
    }

    /**
     * Extrai scopes (permissões) do token.
     *
     * @param token Token JWT
     * @return Lista de scopes (ex: ["pagamentos:read", "pagamentos:write"])
     */
    @SuppressWarnings("unchecked")
    public List<String> extractScopes(String token) {
        Claims claims = extractClaims(token);
        Object scopesObj = claims.get("scopes");
        
        if (scopesObj instanceof List<?>) {
            return (List<String>) scopesObj;
        }
        
        log.warn("⚠️ Token sem scopes válidos");
        return List.of();
    }
}
