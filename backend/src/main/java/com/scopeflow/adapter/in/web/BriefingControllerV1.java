package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Briefing management endpoints for ScopeFlow API (v1).
 *
 * Handles:
 * - Start new briefing session (admin)
 * - List briefing sessions (admin)
 * - Get briefing details (admin)
 * - Complete briefing (admin)
 * - Abandon briefing (admin)
 *
 * All endpoints require JWT authentication.
 * All operations are workspace-scoped (multi-tenancy).
 *
 * Public endpoints (client-facing, token-based) are in PublicBriefingControllerV1.
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/briefing")
@Tag(name = "Briefing (Admin)", description = "AI-assisted discovery flow management (admin endpoints)")
@SecurityRequirement(name = "bearerAuth")
public class BriefingControllerV1 {

    /**
     * Start a new briefing session.
     *
     * Creates an AI-assisted discovery flow for a client and service type.
     * Returns the first question and public token for client access.
     *
     * Invariant: Only 1 active briefing per client per service type.
     *
     * Request example:
     * ```json
     * {
     *   "client_id": "550e8400-e29b-41d4-a716-446655440000",
     *   "service_type": "SOCIAL_MEDIA"
     * }
     * ```
     *
     * Response: 201 CREATED with first question and public access token
     *
     * Error: 409 CONFLICT if active briefing already exists for client+service
     */
    @PostMapping
    @Operation(
            summary = "Start new briefing session",
            description = "Create AI-assisted discovery flow for client. Returns first question and public token."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Briefing session started",
                    content = @Content(schema = @Schema(implementation = StartBriefingResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Active briefing already exists (BRIEFING-006)"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Not member of workspace")
    })
    public ResponseEntity<StartBriefingResponse> startBriefing(
            @PathVariable @Parameter(description = "Workspace UUID") UUID workspaceId,
            @Valid @RequestBody StartBriefingRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * List all briefing sessions in workspace.
     *
     * Filters by status (IN_PROGRESS, COMPLETED, ABANDONED).
     * Returns paginated list with progress metrics.
     *
     * Query params:
     * - page: 0-indexed page number (default: 0)
     * - size: results per page (default: 20, max: 100)
     * - status: filter by status (optional)
     *
     * Response: 200 OK with paginated list
     */
    @GetMapping
    @Operation(
            summary = "List briefing sessions",
            description = "Get all briefing sessions in workspace with progress metrics."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Paginated list of briefing sessions",
            content = @Content(schema = @Schema(implementation = BriefingListResponse.class))
    )
    public ResponseEntity<BriefingListResponse> listBriefings(
            @PathVariable @Parameter(description = "Workspace UUID") UUID workspaceId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @Parameter(description = "Filter by status (IN_PROGRESS, COMPLETED, ABANDONED)") String status
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Get briefing session details.
     *
     * Returns full briefing data including:
     * - Session metadata (client, service, status)
     * - All questions asked
     * - All answers submitted
     * - Progress metrics
     * - AI analysis (if available)
     *
     * Requires: User must be member of workspace
     * Response: 200 OK with full briefing details
     */
    @GetMapping("/{sessionId}")
    @Operation(
            summary = "Get briefing details",
            description = "Retrieve full briefing session with all questions, answers, and progress."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Briefing details",
                    content = @Content(schema = @Schema(implementation = BriefingSessionDetailResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Briefing session not found"),
            @ApiResponse(responseCode = "403", description = "Not member of workspace")
    })
    public ResponseEntity<BriefingSessionDetailResponse> getBriefing(
            @PathVariable @Parameter(description = "Workspace UUID") UUID workspaceId,
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Complete briefing session.
     *
     * Marks briefing as COMPLETED if completion score >= 80%.
     * Locks session for editing and makes it ready for scope generation.
     *
     * Request:
     * ```json
     * {
     *   "force_complete": false
     * }
     * ```
     *
     * Response: 200 OK with completion summary
     *
     * Invariant: Completion score must be >= 80%
     * Error: 409 CONFLICT if completion score < 80% (BRIEFING-005)
     */
    @PostMapping("/{sessionId}/complete")
    @Operation(
            summary = "Complete briefing",
            description = "Mark briefing as COMPLETED. Requires completion score >= 80%."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Briefing completed",
                    content = @Content(schema = @Schema(implementation = BriefingCompletedResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "Completion score < 80% (BRIEFING-005)"),
            @ApiResponse(responseCode = "404", description = "Briefing session not found")
    })
    public ResponseEntity<BriefingCompletedResponse> completeBriefing(
            @PathVariable @Parameter(description = "Workspace UUID") UUID workspaceId,
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId,
            @Valid @RequestBody CompleteBriefingRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Abandon briefing session.
     *
     * Marks briefing as ABANDONED (can be restarted later).
     * User can optionally provide a reason for abandonment.
     *
     * Request:
     * ```json
     * {
     *   "reason": "Client decided to postpone"
     * }
     * ```
     *
     * Response: 200 OK with abandonment confirmation
     */
    @PostMapping("/{sessionId}/abandon")
    @Operation(
            summary = "Abandon briefing",
            description = "Mark briefing as ABANDONED. Can be restarted later."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Briefing abandoned",
                    content = @Content(schema = @Schema(implementation = BriefingAbandonedResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Briefing session not found")
    })
    public ResponseEntity<BriefingAbandonedResponse> abandonBriefing(
            @PathVariable @Parameter(description = "Workspace UUID") UUID workspaceId,
            @PathVariable @Parameter(description = "Briefing session UUID") UUID sessionId,
            @Valid @RequestBody AbandonBriefingRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    // ============ DTOs (Request/Response Records) ============

    /**
     * Request: Start briefing session.
     */
    public record StartBriefingRequest(
            @NotNull(message = "client_id is required")
            @Schema(description = "Client UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID client_id,

            @NotBlank(message = "service_type is required")
            @Schema(description = "Service type", example = "SOCIAL_MEDIA", allowableValues = {
                    "SOCIAL_MEDIA", "LANDING_PAGE", "WEB_DESIGN", "BRANDING", "VIDEO_PRODUCTION", "CONSULTING"
            })
            String service_type
    ) {
    }

    /**
     * Request: Complete briefing.
     */
    public record CompleteBriefingRequest(
            @Schema(description = "Force completion even if score < 80% (use with caution)", example = "false")
            boolean force_complete
    ) {
    }

    /**
     * Request: Abandon briefing.
     */
    public record AbandonBriefingRequest(
            @Schema(description = "Optional reason for abandonment", example = "Client decided to postpone")
            String reason
    ) {
    }

    /**
     * Response: Briefing session started.
     */
    public record StartBriefingResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Public access token (for client link)")
            String public_token,

            @Schema(description = "First question in discovery flow")
            BriefingQuestionDto first_question,

            @Schema(description = "Progress metrics")
            BriefingProgressDto progress
    ) {
    }

    /**
     * Response: Briefing session summary (for list).
     */
    public record BriefingSessionResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Client UUID")
            UUID client_id,

            @Schema(description = "Service type")
            String service_type,

            @Schema(description = "Session status", allowableValues = {"IN_PROGRESS", "COMPLETED", "ABANDONED"})
            String status,

            @Schema(description = "Completion score (0-100)")
            Integer completion_score,

            @Schema(description = "Progress metrics")
            BriefingProgressDto progress,

            @Schema(description = "Created timestamp")
            Instant created_at,

            @Schema(description = "Updated timestamp")
            Instant updated_at
    ) {
    }

    /**
     * Response: Briefing completed.
     */
    public record BriefingCompletedResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Session status (COMPLETED)")
            String status,

            @Schema(description = "Completion score (0-100)")
            int completion_score,

            @Schema(description = "Identified gaps (if any)")
            List<String> gaps_identified,

            @Schema(description = "Ready for scope generation")
            boolean ready_for_scope_generation,

            @Schema(description = "Completion timestamp")
            Instant completed_at
    ) {
    }

    /**
     * Response: Briefing abandoned.
     */
    public record BriefingAbandonedResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Session status (ABANDONED)")
            String status,

            @Schema(description = "Abandonment reason (if provided)")
            String reason,

            @Schema(description = "Abandonment timestamp")
            Instant abandoned_at
    ) {
    }

    /**
     * Response: Full briefing session details.
     */
    public record BriefingSessionDetailResponse(
            @Schema(description = "Briefing session UUID")
            UUID session_id,

            @Schema(description = "Client UUID")
            UUID client_id,

            @Schema(description = "Service type")
            String service_type,

            @Schema(description = "Session status")
            String status,

            @Schema(description = "Completion score (0-100)")
            Integer completion_score,

            @Schema(description = "Progress metrics")
            BriefingProgressDto progress,

            @Schema(description = "All answers submitted")
            List<BriefingAnswerDto> answers,

            @Schema(description = "AI analysis (if available)")
            Map<String, Object> ai_analysis,

            @Schema(description = "Created timestamp")
            Instant created_at,

            @Schema(description = "Updated timestamp")
            Instant updated_at
    ) {
    }

    /**
     * Response: Paginated list of briefing sessions.
     */
    public record BriefingListResponse(
            @Schema(description = "Briefing sessions in page")
            List<BriefingSessionResponse> content,

            @Schema(description = "Current page number (0-indexed)")
            int page,

            @Schema(description = "Page size")
            int size,

            @Schema(description = "Total elements across all pages")
            int total_elements,

            @Schema(description = "Total pages")
            int total_pages
    ) {
    }

    /**
     * DTO: Briefing progress.
     */
    public record BriefingProgressDto(
            @Schema(description = "Current step (0-indexed)")
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
     * DTO: Briefing answer.
     */
    public record BriefingAnswerDto(
            @Schema(description = "Answer UUID")
            UUID answer_id,

            @Schema(description = "Question UUID")
            UUID question_id,

            @Schema(description = "Question text")
            String question_text,

            @Schema(description = "Answer text")
            String answer_text,

            @Schema(description = "Follow-up question generated")
            boolean follow_up_generated,

            @Schema(description = "AI analysis (if available)")
            Map<String, Object> ai_analysis,

            @Schema(description = "Answer submitted timestamp")
            Instant answered_at
    ) {
    }
}
