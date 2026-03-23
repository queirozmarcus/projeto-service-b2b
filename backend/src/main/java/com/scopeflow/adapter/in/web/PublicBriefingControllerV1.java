package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public briefing endpoints for ScopeFlow API (v1).
 *
 * Client-facing endpoints that use public token authentication
 * (no JWT required).
 *
 * Handles:
 * - Get current question (public)
 * - Submit answer (public)
 * - Complete briefing (public)
 *
 * Authentication: query parameter `token` (public_token from briefing session)
 *
 * All public endpoints:
 * - Rate limited (to prevent abuse)
 * - Log IP address and User-Agent (for audit trail)
 * - No workspace context required (token is self-contained)
 */
@RestController
@RequestMapping("/api/v1/briefing")
@Tag(name = "Briefing (Public)", description = "Client-facing discovery flow (token-based access)")
public class PublicBriefingControllerV1 {

    /**
     * Get current question in briefing flow.
     *
     * Returns:
     * - Current unanswered question
     * - Progress metrics (% complete)
     * - Optionally: history of previous answers (if includeHistory=true)
     *
     * Query params:
     * - token: public access token (required)
     * - includeHistory: include previous answers (default: false)
     *
     * Public endpoint (no JWT required).
     *
     * Error: 401 UNAUTHORIZED if token is invalid/expired
     */
    @GetMapping("/{sessionId}")
    @Operation(
            summary = "Get current question (public)",
            description = "Retrieve current unanswered question in briefing flow. Requires public token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Current question and progress",
                    content = @Content(schema = @Schema(implementation = CurrentQuestionResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "404", description = "Briefing session not found")
    })
    public ResponseEntity<CurrentQuestionResponse> getCurrentQuestion(
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId,
            @RequestParam @Parameter(description = "Public access token", required = true) String token,
            @RequestParam(defaultValue = "false") @Parameter(description = "Include answer history") boolean includeHistory
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Submit answer to current question.
     *
     * Validates answer (not empty), saves to database, and:
     * - Detects gaps in response
     * - Generates follow-up question if needed (max 1 per question)
     * - Returns next question (or follow-up if generated)
     *
     * Request:
     * ```json
     * {
     *   "question_id": "550e8400-e29b-41d4-a716-446655440000",
     *   "answer_text": "Marketing professionals aged 25-45, focused on B2B"
     * }
     * ```
     *
     * Query param:
     * - token: public access token (required)
     *
     * Response: 200 OK with next question or follow-up
     *
     * Error:
     * - 400 BAD_REQUEST if answer is empty (BRIEFING-003)
     * - 401 UNAUTHORIZED if token is invalid
     * - 404 NOT_FOUND if session/question not found
     */
    @PostMapping("/{sessionId}/answers")
    @Operation(
            summary = "Submit answer (public)",
            description = "Submit answer to current question. May trigger AI follow-up. Requires public token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Answer submitted, next question returned",
                    content = @Content(schema = @Schema(implementation = SubmitAnswerResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "Empty answer (BRIEFING-003)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "404", description = "Session or question not found")
    })
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId,
            @RequestParam @Parameter(description = "Public access token", required = true) String token,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Complete briefing session (public).
     *
     * Marks briefing as COMPLETED if:
     * - All required questions answered
     * - Completion score >= 80%
     *
     * Query param:
     * - token: public access token (required)
     *
     * Response: 200 OK with completion summary
     *
     * Error:
     * - 409 CONFLICT if completion score < 80% (BRIEFING-005)
     * - 401 UNAUTHORIZED if token is invalid
     */
    @PostMapping("/{sessionId}/complete")
    @Operation(
            summary = "Complete briefing (public)",
            description = "Mark briefing as COMPLETED. Requires completion score >= 80%. Requires public token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Briefing completed",
                    content = @Content(schema = @Schema(implementation = BriefingCompletedPublicResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "Completion score < 80% (BRIEFING-005)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "404", description = "Briefing session not found")
    })
    public ResponseEntity<BriefingCompletedPublicResponse> completeBriefingPublic(
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId,
            @RequestParam @Parameter(description = "Public access token", required = true) String token
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    // ============ DTOs (Request/Response Records) ============

    /**
     * Request: Submit answer.
     */
    public record SubmitAnswerRequest(
            @NotNull(message = "question_id is required")
            @Schema(description = "Question UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID question_id,

            @NotBlank(message = "answer_text is required")
            @Size(max = 5000, message = "answer_text max 5000 characters")
            @Schema(description = "Answer text", example = "Marketing professionals aged 25-45, focused on B2B")
            String answer_text
    ) {
    }

    /**
     * Response: Current question and progress.
     */
    public record CurrentQuestionResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Current unanswered question")
            BriefingQuestionDto current_question,

            @Schema(description = "Progress metrics")
            BriefingProgressDto progress,

            @Schema(description = "Previous answers (if includeHistory=true)")
            List<BriefingAnswerSummaryDto> previous_answers
    ) {
    }

    /**
     * Response: Answer submitted.
     */
    public record SubmitAnswerResponse(
            @Schema(description = "Answer UUID")
            UUID answer_id,

            @Schema(description = "Answer submitted successfully")
            boolean answer_submitted,

            @Schema(description = "Follow-up question generated (if gaps detected)")
            BriefingQuestionDto follow_up_question,

            @Schema(description = "Next question (if no follow-up)")
            BriefingQuestionDto next_question,

            @Schema(description = "Current completion score (0-100)")
            int completion_score,

            @Schema(description = "Progress metrics")
            BriefingProgressDto progress
    ) {
    }

    /**
     * Response: Briefing completed (public).
     */
    public record BriefingCompletedPublicResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Session status (COMPLETED)")
            String status,

            @Schema(description = "Completion score (0-100)")
            int completion_score,

            @Schema(description = "Identified gaps (if any)")
            List<String> gaps_identified,

            @Schema(description = "Completion timestamp")
            Instant completed_at,

            @Schema(description = "Thank you message")
            String message
    ) {
    }

    /**
     * DTO: Briefing progress.
     */
    public record BriefingProgressDto(
            @Schema(description = "Current step (1-indexed)")
            int current_step,

            @Schema(description = "Total steps in discovery flow")
            int total_steps,

            @Schema(description = "Completion percentage (0-100)")
            int completion_percentage
    ) {
    }

    /**
     * DTO: Briefing question.
     */
    public record BriefingQuestionDto(
            @Schema(description = "Question UUID")
            UUID question_id,

            @Schema(description = "Question text")
            String question_text,

            @Schema(description = "Question type", allowableValues = {"OPEN_ENDED", "MULTIPLE_CHOICE", "SCALE"})
            String question_type,

            @Schema(description = "Step number (1-indexed)")
            int step,

            @Schema(description = "Total steps")
            int total_steps
    ) {
    }

    /**
     * DTO: Answer summary (for history).
     */
    public record BriefingAnswerSummaryDto(
            @Schema(description = "Question text")
            String question_text,

            @Schema(description = "Answer text")
            String answer_text,

            @Schema(description = "Follow-up question generated")
            boolean follow_up_generated,

            @Schema(description = "Answer submitted timestamp")
            Instant answered_at
    ) {
    }
}
