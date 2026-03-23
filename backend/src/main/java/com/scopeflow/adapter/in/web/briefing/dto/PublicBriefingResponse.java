package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for public briefing access (no sensitive workspace/client data).
 */
@Schema(description = "Public briefing response (client-facing)")
public record PublicBriefingResponse(

        @Schema(description = "Briefing session UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Service type", example = "SOCIAL_MEDIA")
        String serviceType,

        @Schema(description = "Briefing status", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Progress metrics")
        ProgressResponse progress,

        @Schema(description = "Created timestamp", example = "2026-03-22T10:00:00Z")
        Instant createdAt

) {
}
