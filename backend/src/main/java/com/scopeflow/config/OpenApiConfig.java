package com.scopeflow.config;

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
 * OpenAPI 3.1 configuration for ScopeFlow API.
 *
 * Generates Swagger UI at:
 * - http://localhost:8080/swagger-ui.html
 * - http://localhost:8080/v3/api-docs
 *
 * Configures:
 * - API info (title, version, description, contact)
 * - JWT Bearer authentication
 * - Server configurations (dev, staging, prod)
 * - Security schemes
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String appVersion;

    @Value("${app.environment:development}")
    private String environment;

    /**
     * Define OpenAPI specification for ScopeFlow API.
     */
    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(apiInfo())
                .components(components())
                .servers(servers())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    /**
     * API information (title, description, version, contact, license).
     */
    private Info apiInfo() {
        return new Info()
                .title("ScopeFlow API")
                .version(appVersion)
                .description("AI-powered B2B SaaS platform for streamlining sales proposals.\n\n" +
                        "**Features:**\n" +
                        "- Multi-tenant workspace management\n" +
                        "- AI-assisted briefing discovery\n" +
                        "- Scope generation & proposal management\n" +
                        "- Role-based access control (OWNER, ADMIN, MEMBER)\n" +
                        "- Transactional event sourcing (Outbox pattern)\n" +
                        "- Audit trail & compliance logging\n\n" +
                        "**Architecture:**\n" +
                        "- Hexagonal (ports & adapters)\n" +
                        "- Domain-driven design (DDD)\n" +
                        "- Java 21 (sealed classes, records, virtual threads)\n" +
                        "- PostgreSQL + Flyway (versioned migrations)\n" +
                        "- Event-driven (Kafka/RabbitMQ ready)\n\n" +
                        "**Authentication:**\n" +
                        "All endpoints require JWT bearer token (except POST /auth/register, POST /auth/login)"
                )
                .contact(new Contact()
                        .name("ScopeFlow Support")
                        .email("support@scopeflow.com")
                        .url("https://scopeflow.com")
                )
                .license(new License()
                        .name("Proprietary")
                        .url("https://scopeflow.com/license")
                );
    }

    /**
     * Security schemes (JWT Bearer authentication).
     */
    private Components components() {
        return new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT Bearer token.\n\n" +
                                "Obtain via:\n" +
                                "1. POST /auth/register — create account\n" +
                                "2. POST /auth/login — authenticate with email+password\n\n" +
                                "Response:\n" +
                                "```json\n" +
                                "{\n" +
                                "  \"access_token\": \"eyJhbGc...\",\n" +
                                "  \"refresh_token\": \"eyJhbGc...\",\n" +
                                "  \"expires_in\": 900\n" +
                                "}\n" +
                                "```\n\n" +
                                "Use: `Authorization: Bearer {access_token}`\n" +
                                "Expiration: 15 minutes (access), 7 days (refresh)"
                        )
                );
    }

    /**
     * Server configurations (dev, staging, prod).
     */
    private List<Server> servers() {
        Server development = new Server()
                .url("http://localhost:8080")
                .description("Development (local)");

        Server staging = new Server()
                .url("https://api-staging.scopeflow.com")
                .description("Staging environment");

        Server production = new Server()
                .url("https://api.scopeflow.com")
                .description("Production environment");

        // Return only relevant server based on environment
        if ("production".equals(environment)) {
            return List.of(production, staging);
        } else if ("staging".equals(environment)) {
            return List.of(staging, development);
        } else {
            return List.of(development, staging, production);
        }
    }
}
