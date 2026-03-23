package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for a briefing question.
 */
@Schema(description = "Briefing question response")
public record QuestionResponse(

        @Schema(description = "Question UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Question text", example = "What are your main goals for this social media campaign?")
        String text,

        @Schema(description = "Sequential step number", example = "1")
        int step,

        @Schema(description = "Question type", example = "OPEN_ENDED")
        String questionType,

        @Schema(description = "Is this question required?", example = "true")
        boolean required,

        @Schema(description = "True if this is an auto-generated follow-up question", example = "false")
        boolean followUpGenerated

) {
}
