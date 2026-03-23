package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for briefing progress metrics.
 */
@Schema(description = "Briefing progress metrics")
public record ProgressResponse(

        @Schema(description = "Current step number", example = "7")
        int currentStep,

        @Schema(description = "Total steps", example = "10")
        int totalSteps,

        @Schema(description = "Completion percentage (0-100)", example = "70")
        int completionPercentage,

        @Schema(description = "List of identified gaps", example = "[\"Need more details on target audience\", \"Budget range missing\"]")
        List<String> gapsIdentified

) {
}
