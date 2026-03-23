package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request to submit an answer to a briefing question.
 *
 * Invariants:
 * - Answer text cannot be empty
 * - Max 1 follow-up per question
 */
@Schema(description = "Request to submit an answer to a briefing question")
public record SubmitAnswerRequest(

        @NotNull(message = "Question ID is required")
        @Schema(description = "Question UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7", required = true)
        UUID questionId,

        @NotBlank(message = "Answer text cannot be blank")
        @Size(min = 1, max = 5000, message = "Answer text must be between 1 and 5000 characters")
        @Schema(description = "Client's answer text", example = "We want to post 3 times per week on Instagram and Facebook.", required = true)
        String answerText

) {
}
