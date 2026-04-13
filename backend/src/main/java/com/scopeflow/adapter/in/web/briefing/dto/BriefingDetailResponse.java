package com.scopeflow.adapter.in.web.briefing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for briefing session with full details (progress, questions, answers).
 */
@Schema(description = "Briefing session response with full details")
public record BriefingDetailResponse(

        @Schema(description = "Briefing session UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "Workspace UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID workspaceId,

        @Schema(description = "Client UUID", example = "6ba7b810-9dad-11d1-80b4-00c04fd430c8")
        UUID clientId,

        @Schema(description = "Service type", example = "SOCIAL_MEDIA")
        String serviceType,

        @Schema(description = "Briefing status", example = "IN_PROGRESS")
        String status,

        @Schema(description = "Public token for client access", example = "abc123def456ghi789jkl012mno345pqr678stu901")
        String publicToken,

        @Schema(description = "Completion score (only set when status=COMPLETED)", example = "95")
        Integer completionScore,

        @Schema(description = "Created timestamp", example = "2026-03-22T10:00:00Z")
        Instant createdAt,

        @Schema(description = "Updated timestamp", example = "2026-03-22T14:30:00Z")
        Instant updatedAt,

        @Schema(description = "Progress metrics")
        ProgressResponse progress,

        @Schema(description = "List of questions")
        List<QuestionResponse> questions,

        @Schema(description = "List of answers")
        List<AnswerResponse> answers

) {
}
