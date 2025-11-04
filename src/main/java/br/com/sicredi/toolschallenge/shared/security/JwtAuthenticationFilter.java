package br.com.sicredi.toolschallenge.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta todas as requisições HTTP para validar token JWT.
 * Se token válido, extrai scopes e configura SecurityContext.
 * Se token inválido/ausente, NÃO bloqueia (deixa SecurityConfig tratar 401).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            if (token != null && jwtService.validateToken(token)) {
                authenticateUser(token, request);
            }
        } catch (Exception e) {
            log.error("Erro ao processar autenticação JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai token do header Authorization.
     *
     * @param request HttpServletRequest
     * @return Token JWT (sem prefixo "Bearer ") ou null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        return null;
    }

    /**
     * Configura autenticação no SecurityContext com base no token.
     *
     * @param token Token JWT válido
     * @param request HttpServletRequest
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        String subject = jwtService.extractSubject(token);
        List<String> scopes = jwtService.extractScopes(token);

        // Converter scopes para GrantedAuthorities
        List<SimpleGrantedAuthority> authorities = scopes.stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        // Criar Authentication com scopes
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(subject, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Configurar SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("Usuário autenticado - Subject: {}, Scopes: {}", subject, scopes);
    }
}
