package br.com.sicredi.toolschallenge.shared.config;

import br.com.sicredi.toolschallenge.infra.idempotencia.interceptor.IdempotenciaInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração para registrar o interceptor de idempotência.
 */
@Configuration
@RequiredArgsConstructor
public class IdempotenciaConfig implements WebMvcConfigurer {

    private final IdempotenciaInterceptor idempotenciaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotenciaInterceptor)
                .addPathPatterns("/pagamentos/**", "/estornos/**")
                .excludePathPatterns("/actuator/**");
    }
}
