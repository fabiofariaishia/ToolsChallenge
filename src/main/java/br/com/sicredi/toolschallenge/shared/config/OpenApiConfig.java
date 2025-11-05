package br.com.sicredi.toolschallenge.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuração centralizada do OpenAPI/Swagger.
 * 
 * Define:
 * - Informações gerais da API (título, versão, descrição, contato)
 * - Security Scheme JWT Bearer para autenticação
 * - Servidor de desenvolvimento
 * - Licença e termos de uso
 * 
 * Acesso: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:toolschallenge}")
    private String applicationName;

    /**
     * Configura OpenAPI com metadados da API e security scheme JWT.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Nome do security scheme (referenciado em @SecurityRequirement)
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // Informações gerais da API
                .info(apiInfo())
                
                // Servidor de desenvolvimento
                .servers(List.of(
                    new Server()
                        .url("http://localhost:8080")
                        .description("Ambiente de Desenvolvimento")
                ))
                
                // Security Scheme: JWT Bearer Token
                .components(new Components()
                    .addSecuritySchemes(securitySchemeName, 
                        new SecurityScheme()
                            .name(securitySchemeName)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Insira o token JWT obtido no endpoint de autenticação. " +
                                       "Formato: Bearer {token}")
                    )
                )
                
                // Aplica security globalmente (todos endpoints exigem JWT)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    /**
     * Metadados da API.
     */
    private Info apiInfo() {
        return new Info()
                .title("ToolsChallenge - API de Pagamentos Sicredi")
                .version("0.0.1-SNAPSHOT")
                .description(
                    "API REST de processamento de pagamentos com cartão, desenvolvida para o Sicredi.\n\n" +
                    "**Características principais:**\n" +
                    "- ✅ Autorização de pagamentos em tempo real\n" +
                    "- ✅ Estorno total de transações autorizadas\n" +
                    "- ✅ Idempotência para evitar duplicação de transações\n" +
                    "- ✅ Auditoria completa de eventos de negócio\n" +
                    "- ✅ Resiliência com Circuit Breaker, Retry e Bulkhead\n" +
                    "- ✅ Autenticação JWT com controle de permissões (scopes)\n\n" +
                    "**Arquitetura:**\n" +
                    "- Monolito Modular preparado para evolução a Microserviços\n" +
                    "- Event-Driven com Kafka (Outbox Pattern)\n" +
                    "- Locks Distribuídos com Redisson (Redis)\n" +
                    "- Observabilidade com Prometheus, Grafana e Jaeger\n\n" +
                    "**Como usar:**\n" +
                    "1. Obtenha um token JWT (endpoint de autenticação - Admin)\n" +
                    "2. Clique em 'Authorize' e insira o token gerado" +
                    "3. Teste os endpoints de Pagamentos e Estornos\n" +
                    "4. Use header `Chave-Idempotencia` (UUID) em operações POST"
                );
    }
}
