package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check endpoints for ScopeFlow API.
 *
 * Used by:
 * - Kubernetes liveness probe (GET /health/live)
 * - Kubernetes readiness probe (GET /health/ready)
 * - Load balancer health checks
 * - Monitoring systems (Prometheus, DataDog, etc.)
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "API health status endpoints")
public class HealthController {

    private final HealthEndpoint healthEndpoint;

    public HealthController(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    /**
     * Liveness probe: is the application running?
     *
     * Returns 200 if process is alive. Simple memory/CPU check.
     * Used by Kubernetes to detect dead processes.
     *
     * @return HTTP 200 if alive
     */
    @GetMapping("/live")
    @Operation(
            summary = "Liveness probe",
            description = "Check if application is alive (process running). Used by Kubernetes liveness probe."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Application is alive",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
    )
    public ResponseEntity<HealthResponse> live() {
        return ResponseEntity.ok(new HealthResponse("UP", "Application is running"));
    }

    /**
     * Readiness probe: is the application ready to accept traffic?
     *
     * Checks:
     * - Database connectivity (Flyway migrations applied)
     * - Message queue connectivity (RabbitMQ / Kafka)
     * - External services reachable (S3, OpenAI API)
     *
     * Returns 503 if dependencies are down.
     * Used by Kubernetes to route traffic away from unhealthy instances.
     *
     * @return HTTP 200 if ready, 503 if not ready
     */
    @GetMapping("/ready")
    @Operation(
            summary = "Readiness probe",
            description = "Check if application is ready to accept traffic. Validates database, message queue, and external dependencies."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Application is ready",
            content = @Content(schema = @Schema(implementation = HealthResponse.class))
    )
    @ApiResponse(
            responseCode = "503",
            description = "Application is not ready (database, queue, or external service unavailable)"
    )
    public ResponseEntity<HealthResponse> ready() {
        HealthComponent health = healthEndpoint.health();
        String status = health.getStatus().toString();

        if ("UP".equals(status)) {
            return ResponseEntity.ok(new HealthResponse("UP", "Application is ready to accept traffic"));
        } else {
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new HealthResponse("DOWN", "Application dependencies not ready"));
        }
    }

    /**
     * Full health details: deep status of all components.
     *
     * Returns:
     * - Application status
     * - Database connectivity
     * - Message queue status
     * - External service status (S3, OpenAI)
     * - JVM memory usage
     * - Uptime
     *
     * Used for debugging and detailed monitoring.
     *
     * @return HTTP 200 with full health details
     */
    @GetMapping("/details")
    @Operation(
            summary = "Detailed health status",
            description = "Get detailed health information including database, message queue, external services, and JVM metrics."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Detailed health information",
            content = @Content(schema = @Schema(implementation = HealthComponent.class))
    )
    public ResponseEntity<HealthComponent> details() {
        return ResponseEntity.ok(healthEndpoint.health());
    }

    /**
     * Health response record.
     */
    public record HealthResponse(
            String status,
            String message
    ) {
    }
}
