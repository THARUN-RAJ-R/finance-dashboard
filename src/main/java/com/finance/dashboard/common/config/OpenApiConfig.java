package com.finance.dashboard.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger UI configuration.
 * Exposes the API under /swagger-ui.html with JWT Bearer auth pre-configured.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Finance Dashboard API")
                        .version("1.0.0")
                        .description("""
                                ## Finance Dashboard REST API
                                
                                A production-quality backend for managing financial records with
                                role-based access control (RBAC).
                                
                                ### Roles
                                | Role | Permissions |
                                |------|-------------|
                                | VIEWER | Dashboard summary endpoints only |
                                | ANALYST | Dashboard + read financial records |
                                | ADMIN | Full access including user management |
                                
                                ### Authentication
                                1. Call `POST /api/v1/auth/login` with valid credentials
                                2. Copy the `accessToken` from the response
                                3. Click **Authorize** and paste `<token>` (without "Bearer ")
                                
                                ### Demo Credentials
                                | Email | Password | Role |
                                |-------|----------|------|
                                | admin@local | admin123 | ADMIN |
                                | analyst@local | analyst123 | ANALYST |
                                | viewer@local | viewer123 | VIEWER |
                                """))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)
                                   .description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token obtained from /auth/login")));
    }
}
