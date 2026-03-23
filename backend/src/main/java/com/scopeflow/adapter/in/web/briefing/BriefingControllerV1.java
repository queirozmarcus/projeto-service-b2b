package com.scopeflow.adapter.in.web.briefing;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.adapter.in.web.briefing.mapper.BriefingMapper;
import com.scopeflow.core.domain.briefing.BriefingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for authenticated Briefing endpoints.
 *
 * All endpoints require JWT authentication.
 * Workspace ID is extracted from JWT claims.
 *
 * Rate limit: 100 req/min per user.
 */
@RestController
@RequestMapping("/api/v1/briefings")
@Tag(name = "Briefings", description = "Authenticated endpoints for workspace owners/members to manage briefings")
@SecurityRequirement(name = "bearerAuth")
public class BriefingControllerV1 {

    private final BriefingService briefingService;
    private final BriefingMapper mapper;

    public BriefingControllerV1(BriefingService briefingService, BriefingMapper mapper) {
        this.briefingService = briefingService;
        this.mapper = mapper;
    }

    /**
     * POST /api/v1/briefings - Create a new briefing session.
     *
     * Invariant: Only 1 active briefing per client per service type per workspace.
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param request CreateBriefingRequest
     * @return 201 Created with BriefingResponse
     * @throws BriefingAlreadyInProgressException if duplicate active session
     */
    @PostMapping
    @Operation(
            summary = "Create a new briefing session",
            description = "Starts a new discovery flow for a client and service type. Invariant: Only 1 active briefing per client per service type."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Briefing session created successfully",
                    content = @Content(schema = @Schema(implementation = BriefingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Duplicate active briefing"),
            @ApiResponse(responseCode = "422", description = "Business rule violation"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<BriefingResponse> createBriefing(
            @Valid @RequestBody CreateBriefingRequest request
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT SecurityContext
        // 2. Convert DTO → domain: clientId, serviceType
        // 3. Call briefingService.startBriefing(workspaceId, clientId, serviceType)
        // 4. Convert domain → DTO: BriefingInProgress → BriefingResponse
        // 5. Return ResponseEntity.status(201).header("Location", "/api/v1/briefings/{id}").body(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * GET /api/v1/briefings - List briefings with pagination and filters.
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param status Filter by status (optional)
     * @param serviceType Filter by service type (optional)
     * @param createdAfter Filter by creation date (optional)
     * @param page Page number (zero-based, default 0)
     * @param size Page size (default 20, max 100)
     * @param sort Sort field and direction (default "createdAt,desc")
     * @return 200 OK with PageResponse<BriefingResponse>
     */
    @GetMapping
    @Operation(
            summary = "List briefings with pagination and filters",
            description = "Returns paginated list of briefings for the authenticated user's workspace."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of briefings",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<PageResponse<BriefingResponse>> listBriefings(
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by service type") @RequestParam(required = false) String serviceType,
            @Parameter(description = "Filter by creation date (ISO 8601)") @RequestParam(required = false) String createdAfter,
            @Parameter(description = "Page number (zero-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field and direction") @RequestParam(defaultValue = "createdAt,desc") String sort
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Build Pageable + filters
        // 3. Call repository.findByWorkspace(workspaceId, filters, pageable)
        // 4. Convert domain → DTO: Page<BriefingSession> → PageResponse<BriefingResponse>
        // 5. Return ResponseEntity.ok(pageResponse)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * GET /api/v1/briefings/{briefingId} - Get briefing details.
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param briefingId Briefing session UUID
     * @return 200 OK with BriefingDetailResponse
     * @throws BriefingNotFoundException if session not found
     */
    @GetMapping("/{briefingId}")
    @Operation(
            summary = "Get briefing details",
            description = "Returns full details of a briefing session."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Briefing details",
                    content = @Content(schema = @Schema(implementation = BriefingDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<BriefingDetailResponse> getBriefing(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert briefingId → BriefingSessionId
        // 3. Call briefingService or repository.findById(briefingSessionId)
        // 4. Verify ownership: session.workspaceId == JWT workspaceId
        // 5. Fetch questions + answers
        // 6. Convert domain → DTO: BriefingDetailResponse
        // 7. Return ResponseEntity.ok(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * GET /api/v1/briefings/{briefingId}/progress - Get briefing progress metrics.
     *
     * Rate limit: 100 req/min (authenticated)
     * Caching: 30s (progress updates on answer submission)
     *
     * @param briefingId Briefing session UUID
     * @return 200 OK with ProgressResponse
     * @throws BriefingNotFoundException if session not found
     */
    @GetMapping("/{briefingId}/progress")
    @Operation(
            summary = "Get briefing progress metrics",
            description = "Returns completion progress (step count, percentage, gaps). Cached for 30s."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progress metrics",
                    content = @Content(schema = @Schema(implementation = ProgressResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing not found"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<ProgressResponse> getBriefingProgress(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert briefingId → BriefingSessionId
        // 3. Call briefingService.detectGaps(briefingSessionId) or fetch from session
        // 4. Convert domain → DTO: BriefingProgress/CompletionScore → ProgressResponse
        // 5. Return ResponseEntity.ok().cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS)).body(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * GET /api/v1/briefings/{briefingId}/next-question - Get next question in the flow.
     *
     * Invariant: Questions are sequential (no skip).
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param briefingId Briefing session UUID
     * @return 200 OK with QuestionResponse
     * @throws BriefingNotFoundException if session not found
     * @throws InvalidStateException if session not in progress or no more questions
     */
    @GetMapping("/{briefingId}/next-question")
    @Operation(
            summary = "Get next question in the flow",
            description = "Returns the next sequential question to answer. Invariant: Questions are sequential (no skip)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Next question",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing not found"),
            @ApiResponse(responseCode = "409", description = "Session not in progress or no more questions"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<QuestionResponse> getNextQuestion(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert briefingId → BriefingSessionId
        // 3. Call briefingService.getNextQuestion(briefingSessionId)
        // 4. Convert domain → DTO: BriefingQuestion → QuestionResponse
        // 5. Return ResponseEntity.ok(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * POST /api/v1/briefings/{briefingId}/answers - Submit answer to a question.
     *
     * Invariants:
     * - Answer text cannot be empty
     * - Max 1 follow-up per question
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param briefingId Briefing session UUID
     * @param request SubmitAnswerRequest
     * @return 204 No Content
     * @throws InvalidAnswerException if answer invalid
     * @throws MaxFollowupExceededException if follow-up limit exceeded
     */
    @PostMapping("/{briefingId}/answers")
    @Operation(
            summary = "Submit answer to a question",
            description = "Submits a client answer to a briefing question. Invariants: Answer cannot be empty, max 1 follow-up per question."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Answer submitted successfully (no content)"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing or question not found"),
            @ApiResponse(responseCode = "409", description = "Session not in progress"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (e.g., max follow-up exceeded)"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> submitAnswer(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert DTO → domain: briefingId, questionId, answerText
        // 3. Call briefingService.submitDirectAnswer(sessionId, questionId, answerText, qualityScore)
        // 4. Return ResponseEntity.noContent().build()
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * POST /api/v1/briefings/{briefingId}/complete - Mark briefing as completed.
     *
     * Invariant: Completion score must be >= 80% and no critical gaps.
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param briefingId Briefing session UUID
     * @param request CompleteBriefingRequest
     * @return 200 OK with BriefingResponse
     * @throws IncompleteGapsException if score < 80%
     * @throws BriefingAlreadyCompletedException if already completed
     */
    @PostMapping("/{briefingId}/complete")
    @Operation(
            summary = "Mark briefing as completed",
            description = "Completes the briefing (locks it for scope generation). Invariant: Completion score >= 80%."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Briefing completed successfully",
                    content = @Content(schema = @Schema(implementation = BriefingResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing not found"),
            @ApiResponse(responseCode = "409", description = "Briefing already completed"),
            @ApiResponse(responseCode = "422", description = "Completion score < 80%"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<BriefingResponse> completeBriefing(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId,
            @Valid @RequestBody CompleteBriefingRequest request
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert DTO → domain: briefingId, CompletionScore
        // 3. Call briefingService.completeBriefing(sessionId, score)
        // 4. Convert domain → DTO: BriefingCompleted → BriefingResponse
        // 5. Return ResponseEntity.ok(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * POST /api/v1/briefings/{briefingId}/abandon - Abandon briefing session.
     *
     * Client can start a new briefing later.
     *
     * Rate limit: 100 req/min (authenticated)
     *
     * @param briefingId Briefing session UUID
     * @param request AbandonBriefingRequest
     * @return 204 No Content
     * @throws BriefingNotFoundException if session not found
     * @throws BriefingAlreadyCompletedException if already completed
     */
    @PostMapping("/{briefingId}/abandon")
    @Operation(
            summary = "Abandon briefing session",
            description = "Abandons an in-progress briefing. Client can start a new one later."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Briefing abandoned successfully (no content)"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Briefing not found"),
            @ApiResponse(responseCode = "409", description = "Cannot abandon completed briefing"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> abandonBriefing(
            @Parameter(description = "Briefing session UUID") @PathVariable UUID briefingId,
            @Valid @RequestBody AbandonBriefingRequest request
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Extract workspace_id from JWT
        // 2. Convert briefingId → BriefingSessionId
        // 3. Call briefingService.abandonBriefing(sessionId)
        // 4. Return ResponseEntity.noContent().build()
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
