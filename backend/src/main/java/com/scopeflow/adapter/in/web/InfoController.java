package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Info endpoints for ScopeFlow API.
 *
 * Provides version, environment, and build information.
 * Used by:
 * - Client applications to verify API version compatibility
 * - Deployment monitoring (verify correct version deployed)
 * - Debugging (check environment, git commit, build time)
 */
@RestController
@RequestMapping("/api/v1/info")
@Tag(name = "Info", description = "API information and version endpoints")
public class InfoController {

    @Value("${app.version:1.0.0-SNAPSHOT}")
    private String version;

    @Value("${app.name:ScopeFlow API}")
    private String appName;

    @Value("${app.environment:development}")
    private String environment;

    @Value("${git.commit.id.abbrev:unknown}")
    private String gitCommit;

    @Value("${git.build.time:unknown}")
    private String buildTime;

    /**
     * Get API information: version, name, environment.
     *
     * Response example:
     * {
     *   "app_name": "ScopeFlow API",
     *   "version": "1.0.0-sprint1",
     *   "environment": "staging",
     *   "git_commit": "a1b2c3d",
     *   "build_time": "2026-03-22T15:30:00Z",
     *   "timestamp": "2026-03-22T16:45:23Z"
     * }
     *
     * @return HTTP 200 with info
     */
    @GetMapping
    @Operation(
            summary = "Get API information",
            description = "Returns version, environment, build information, and deployment metadata."
    )
    @ApiResponse(
            responseCode = "200",
            description = "API information",
            content = @Content(schema = @Schema(implementation = ApiInfoResponse.class))
    )
    public ResponseEntity<ApiInfoResponse> info() {
        ApiInfoResponse response = new ApiInfoResponse(
                appName,
                version,
                environment,
                gitCommit,
                buildTime,
                Instant.now()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Get API capabilities: supported features, bounded contexts, agents.
     *
     * Response example:
     * {
     *   "api_version": "v1",
     *   "bounded_contexts": ["user-workspace", "briefing", "proposal"],
     *   "features": {
     *     "authentication": "JWT + Spring Security",
     *     "multi_tenancy": "workspace-scoped",
     *     "event_sourcing": "Outbox pattern (Kafka/RabbitMQ)",
     *     "audit_trail": "immutable activity_logs"
     *   },
     *   "architecture": "hexagonal + DDD"
     * }
     *
     * @return HTTP 200 with capabilities
     */
    @GetMapping("/capabilities")
    @Operation(
            summary = "Get API capabilities",
            description = "Returns supported features, bounded contexts, and architectural patterns."
    )
    @ApiResponse(
            responseCode = "200",
            description = "API capabilities",
            content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<Map<String, Object>> capabilities() {
        Map<String, Object> capabilities = new HashMap<>();

        capabilities.put("api_version", "v1");

        capabilities.put("bounded_contexts", new String[]{
                "user-workspace",
                "briefing",
                "proposal"
        });

        Map<String, String> features = new HashMap<>();
        features.put("authentication", "JWT + Spring Security 6.x");
        features.put("multi_tenancy", "workspace-scoped queries");
        features.put("event_sourcing", "Outbox pattern (Kafka/RabbitMQ ready)");
        features.put("audit_trail", "immutable activity_logs table");
        features.put("type_safety", "Java 21 sealed classes + records");
        features.put("async_processing", "virtual threads + Spring Integration");
        capabilities.put("features", features);

        Map<String, String> architecture = new HashMap<>();
        architecture.put("pattern", "Hexagonal (ports & adapters)");
        architecture.put("domain_driven_design", "DDD + bounded contexts");
        architecture.put("database", "PostgreSQL 16 + Flyway");
        architecture.put("cache", "Redis (optional)");
        architecture.put("queue", "RabbitMQ / Kafka");
        capabilities.put("architecture", architecture);

        return ResponseEntity.ok(capabilities);
    }

    /**
     * API info response record (OpenAPI schema).
     */
    public record ApiInfoResponse(
            String app_name,
            String version,
            String environment,
            String git_commit,
            String build_time,
            Instant timestamp
    ) {
    }
}
