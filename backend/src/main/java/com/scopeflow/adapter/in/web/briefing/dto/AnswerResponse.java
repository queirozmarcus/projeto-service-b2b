package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a briefing answer.
 */
@Schema(description = "Briefing answer response")
public record AnswerResponse(

        @Schema(description = "Answer UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Question UUID", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        UUID questionId,

        @Schema(description = "Answer text", example = "We want to increase brand awareness and generate leads for our wellness coaching service.")
        String answerText,

        @Schema(description = "AI-computed quality score (null if not yet computed)", example = "85")
        Integer qualityScore,

        @Schema(description = "Answer submission timestamp", example = "2026-03-22T11:15:00Z")
        Instant createdAt

) {
}
