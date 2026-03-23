package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request to mark briefing as completed.
 *
 * Invariant: Completion score must be >= 80% and no critical gaps.
 */
@Schema(description = "Request to mark briefing as completed")
public record CompleteBriefingRequest(

        @NotNull(message = "Completion score is required")
        @Min(value = 80, message = "Completion score must be at least 80")
        @Max(value = 100, message = "Completion score cannot exceed 100")
        @Schema(description = "Completion score (must be >= 80 to complete)", example = "95", required = true)
        Integer completionScore,

        @Schema(description = "List of identified gaps (optional)", example = "[\"Audience demographics unclear\", \"Budget range not specified\"]")
        List<String> gapsIdentified

) {
}
