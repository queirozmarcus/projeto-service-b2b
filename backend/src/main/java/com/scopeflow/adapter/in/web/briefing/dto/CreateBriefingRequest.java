package com.scopeflow.adapter.in.web.briefing.dto;

import com.scopeflow.core.domain.briefing.ServiceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create a new briefing session.
 *
 * Invariant: Only 1 active briefing per client per service type per workspace.
 */
@Schema(description = "Request to create a new briefing session")
public record CreateBriefingRequest(

        @NotNull(message = "Client ID is required")
        @Schema(description = "Client UUID (from Clients context)", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
        UUID clientId,

        @NotNull(message = "Service type is required")
        @Schema(description = "Type of service", example = "SOCIAL_MEDIA", required = true)
        ServiceType serviceType

) {
}
