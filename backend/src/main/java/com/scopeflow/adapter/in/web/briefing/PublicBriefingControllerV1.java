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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for public (no auth) Briefing endpoints.
 *
 * Client-facing endpoints: clients access briefing via public token.
 * No JWT authentication required.
 *
 * Rate limit: 10 req/min per IP (public endpoints).
 */
@RestController
@RequestMapping("/public/briefings")
@Tag(name = "Public Briefings", description = "Public endpoints for clients to access and answer briefing questions (no authentication)")
public class PublicBriefingControllerV1 {

    private final BriefingService briefingService;
    private final BriefingMapper mapper;

    public PublicBriefingControllerV1(BriefingService briefingService, BriefingMapper mapper) {
        this.briefingService = briefingService;
        this.mapper = mapper;
    }

    /**
     * GET /public/briefings/{publicToken} - Get public briefing by token (no auth).
     *
     * Client-facing endpoint. Returns briefing details via public token.
     *
     * Rate limit: 10 req/min (public, per IP)
     *
     * @param publicToken Public token for client access
     * @return 200 OK with PublicBriefingResponse
     * @throws BriefingNotFoundException if token invalid or session not found
     */
    @GetMapping("/{publicToken}")
    @Operation(
            summary = "Get public briefing by token (no auth)",
            description = "Client-facing endpoint. Returns briefing details via public token. No authentication required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public briefing details",
                    content = @Content(schema = @Schema(implementation = PublicBriefingResponse.class))),
            @ApiResponse(responseCode = "404", description = "Briefing not found or token invalid"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<PublicBriefingResponse> getPublicBriefing(
            @Parameter(description = "Public token for client access") @PathVariable UUID publicToken
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Convert publicToken → PublicToken domain object
        // 2. Call repository.findByPublicToken(publicToken)
        // 3. Convert domain → DTO: BriefingSession → PublicBriefingResponse (no sensitive data)
        // 4. Return ResponseEntity.ok(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * GET /public/briefings/{publicToken}/next-question - Get next question (public, no auth).
     *
     * Client-facing endpoint. Returns next question in the flow.
     *
     * Rate limit: 10 req/min (public, per IP)
     *
     * @param publicToken Public token for client access
     * @return 200 OK with QuestionResponse
     * @throws BriefingNotFoundException if token invalid or session not found
     * @throws InvalidStateException if session not in progress or no more questions
     */
    @GetMapping("/{publicToken}/next-question")
    @Operation(
            summary = "Get next question (public, no auth)",
            description = "Client-facing endpoint. Returns next question in the flow. No authentication required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Next question",
                    content = @Content(schema = @Schema(implementation = QuestionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Briefing not found or token invalid"),
            @ApiResponse(responseCode = "409", description = "Session not in progress or no more questions"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<QuestionResponse> getPublicNextQuestion(
            @Parameter(description = "Public token for client access") @PathVariable UUID publicToken
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Convert publicToken → PublicToken domain object
        // 2. Call repository.findByPublicToken(publicToken) → get session
        // 3. Call briefingService.getNextQuestion(sessionId)
        // 4. Convert domain → DTO: BriefingQuestion → QuestionResponse
        // 5. Return ResponseEntity.ok(response)
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * POST /public/briefings/{publicToken}/answers - Submit answer (public, no auth).
     *
     * Client-facing endpoint. Submits an answer to a question.
     *
     * Rate limit: 10 req/min (public, per IP)
     *
     * @param publicToken Public token for client access
     * @param request SubmitAnswerRequest
     * @return 204 No Content
     * @throws InvalidAnswerException if answer invalid
     * @throws MaxFollowupExceededException if follow-up limit exceeded
     */
    @PostMapping("/{publicToken}/answers")
    @Operation(
            summary = "Submit answer (public, no auth)",
            description = "Client-facing endpoint. Submits an answer to a question. No authentication required."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Answer submitted successfully (no content)"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Briefing not found or token invalid"),
            @ApiResponse(responseCode = "409", description = "Session not in progress"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (e.g., max follow-up exceeded)"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    public ResponseEntity<Void> submitPublicAnswer(
            @Parameter(description = "Public token for client access") @PathVariable UUID publicToken,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        // TODO: Implementation in Step 5 (backend-dev)
        // 1. Convert publicToken → PublicToken domain object
        // 2. Call repository.findByPublicToken(publicToken) → get session
        // 3. Convert DTO → domain: questionId, answerText
        // 4. Call briefingService.submitDirectAnswer(sessionId, questionId, answerText, qualityScore)
        // 5. Return ResponseEntity.noContent().build()
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
