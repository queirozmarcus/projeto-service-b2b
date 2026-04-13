package com.scopeflow.adapter.in.web.briefing;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestion;
import com.scopeflow.core.application.briefing.BriefingSessionService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller for BriefingSession discovery flow (Sprint 6 Task 3).
 *
 * <p>All endpoints require JWT authentication. Workspace isolation is enforced
 * via the authenticated token — every operation validates that the resource belongs
 * to the authenticated workspace.
 *
 * <p>Route hierarchy:
 * <ul>
 *   <li>POST   /api/v1/proposals/{proposalId}/briefing-sessions — create session</li>
 *   <li>GET    /api/v1/briefing-sessions/{id}            — get session details</li>
 *   <li>GET    /api/v1/briefing-sessions/{id}/questions  — list template questions</li>
 *   <li>POST   /api/v1/briefing-sessions/{id}/answers    — submit answers (batch)</li>
 *   <li>POST   /api/v1/briefing-sessions/{id}/complete   — complete session</li>
 *   <li>GET    /api/v1/briefing-sessions/token/{token}   — get session by public token</li>
 * </ul>
 */
@RestController
@Tag(name = "BriefingSessions", description = "Discovery flow linked to a proposal")
@SecurityRequirement(name = "bearerAuth")
public class BriefingSessionControllerV2 {

    private final BriefingSessionService service;

    public BriefingSessionControllerV2(BriefingSessionService service) {
        this.service = service;
    }

    // ============ Create ============

    @PostMapping("/api/v1/proposals/{proposalId}/briefing-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a briefing session for a proposal",
            description = "Starts a new discovery flow linked to a proposal. Inherits serviceType from the existing linked briefing or falls back to SOCIAL_MEDIA."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created",
                    content = @Content(schema = @Schema(implementation = BriefingSessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Proposal belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Proposal not found")
    })
    public ResponseEntity<BriefingSessionResponse> createBriefingSession(
            @Parameter(description = "Proposal UUID") @PathVariable UUID proposalId
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        JpaBriefingSession session = service.createBriefingSession(proposalId, workspaceId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(session, proposalId));
    }

    // ============ Read ============

    @GetMapping("/api/v1/briefing-sessions/{id}")
    @Operation(
            summary = "Get briefing session details",
            description = "Returns session state, status, and completeness score (if completed)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session details",
                    content = @Content(schema = @Schema(implementation = BriefingSessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Session belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<BriefingSessionResponse> getBriefingSession(
            @Parameter(description = "BriefingSession UUID") @PathVariable UUID id
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        JpaBriefingSession session = service.getBriefingSession(id, workspaceId);
        return ResponseEntity.ok(toResponse(session, null));
    }

    @GetMapping("/api/v1/briefing-sessions/{id}/questions")
    @Operation(
            summary = "List questions for this briefing session",
            description = "Returns template questions from the ServiceContextProfile configured for the session's service type. Empty list if no profile exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Session belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Session not found")
    })
    public ResponseEntity<List<BriefingQuestionResponse>> getQuestions(
            @Parameter(description = "BriefingSession UUID") @PathVariable UUID id
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        List<JpaServiceContextQuestion> questions = service.getQuestions(id, workspaceId);
        return ResponseEntity.ok(questions.stream().map(this::toQuestionResponse).toList());
    }

    @GetMapping("/api/v1/briefing-sessions/token/{token}")
    @Operation(
            summary = "Get briefing session by public token (authenticated)",
            description = "Workspace-auth access by public token. For unauthenticated client access, use the public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session details"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Session belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Token invalid or session not found")
    })
    public ResponseEntity<BriefingSessionResponse> getByPublicToken(
            @Parameter(description = "Public token") @PathVariable String token
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        JpaBriefingSession session = service.getByPublicToken(token);

        // Workspace isolation: ensure authenticated user can only access sessions in their workspace
        if (!session.getWorkspaceId().equals(workspaceId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "BriefingSession does not belong to the authenticated workspace");
        }

        return ResponseEntity.ok(toResponse(session, null));
    }

    // ============ Commands ============

    @PostMapping("/api/v1/briefing-sessions/{id}/answers")
    @Operation(
            summary = "Submit answers to briefing questions",
            description = "Batch submit answers. Idempotent: already-answered questions are skipped."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Answers submitted"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Session belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "409", description = "Session is not IN_PROGRESS")
    })
    public ResponseEntity<Void> submitAnswers(
            @Parameter(description = "BriefingSession UUID") @PathVariable UUID id,
            @Valid @RequestBody SubmitAnswersRequest request
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        List<BriefingSessionService.AnswerInput> inputs = request.answers().stream()
                .map(a -> new BriefingSessionService.AnswerInput(a.questionId(), a.answerText()))
                .toList();
        service.submitAnswers(id, workspaceId, inputs);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/briefing-sessions/{id}/complete")
    @Operation(
            summary = "Complete the briefing session",
            description = "Marks the session as COMPLETED and calculates the completeness score based on required questions answered."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session completed",
                    content = @Content(schema = @Schema(implementation = BriefingCompletionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Session belongs to a different workspace"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "409", description = "Session is not IN_PROGRESS")
    })
    public ResponseEntity<BriefingCompletionResponse> completeBriefingSession(
            @Parameter(description = "BriefingSession UUID") @PathVariable UUID id
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        BriefingSessionService.CompletionResult result = service.completeBriefingSession(id, workspaceId);

        String message = result.completenessScore() >= 80
                ? "Briefing completed successfully. Ready for scope generation."
                : "Briefing completed with low score (" + result.completenessScore() + "%). Consider revisiting key questions.";

        return ResponseEntity.ok(new BriefingCompletionResponse(
                result.completenessScore(),
                result.status(),
                message
        ));
    }

    // ============ Mappers ============

    private BriefingSessionResponse toResponse(JpaBriefingSession session, UUID proposalId) {
        return new BriefingSessionResponse(
                session.getId(),
                proposalId,
                session.getStatus(),
                session.getPublicToken(),
                session.getCompletionScore(),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private BriefingQuestionResponse toQuestionResponse(JpaServiceContextQuestion q) {
        return new BriefingQuestionResponse(
                q.getId(),
                q.getQuestionText(),
                q.getQuestionType(),
                q.getOrderIndex(),
                q.isRequired()
        );
    }
}
