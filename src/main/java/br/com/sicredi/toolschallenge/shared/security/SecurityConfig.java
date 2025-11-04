package br.com.sicredi.toolschallenge.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança Spring Security com JWT.
 * - STATELESS: Sem sessões (JWT em cada request)
 * - Endpoints públicos: Actuator, Swagger, Admin Tokens
 * - Demais endpoints: Protegidos por JWT + @PreAuthorize
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("🔐 Configurando Spring Security com JWT...");

        http
            // CSRF desabilitado (API stateless com JWT)
            .csrf(AbstractHttpConfigurer::disable)

            // Session STATELESS (sem sessões HTTP)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Configurar endpoints públicos/protegidos
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (sem autenticação)
                .requestMatchers("/atuador/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/admin/tokens/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/admin/tokens/**").permitAll()
                
                // Demais endpoints requerem autenticação
                .anyRequest().authenticated()
            )

            // Adicionar filtro JWT ANTES de UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("✅ Spring Security configurado - Modo: STATELESS, JWT: Ativo");
        return http.build();
    }
}
