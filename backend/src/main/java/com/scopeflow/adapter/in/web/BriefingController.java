package com.scopeflow.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BriefingController — Briefing Sessions & IA-powered Q&A.
 *
 * Endpoints: create session, get questions, submit answers, get status, complete session.
 */
@RestController
@RequestMapping("/projects/{projectId}/briefing")
public class BriefingController {

  /**
   * POST /projects/{projectId}/briefing — Start new briefing session.
   *
   * @param projectId UUID of project
   * @param request CreateBriefingRequest
   * @return BriefingSessionResponse
   */
  @PostMapping
  public ResponseEntity<BriefingSessionResponse> startBriefing(
      @PathVariable UUID projectId, @RequestBody CreateBriefingRequest request) {
    // TODO: Implement briefing session creation logic
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * GET /projects/{projectId}/briefing/{sessionId} — Get briefing session status.
   *
   * @param projectId UUID of project
   * @param sessionId UUID of briefing session
   * @return BriefingSessionResponse
   */
  @GetMapping("/{sessionId}")
  public ResponseEntity<BriefingSessionResponse> getBriefingSession(
      @PathVariable UUID projectId, @PathVariable UUID sessionId) {
    // TODO: Implement fetch session logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /projects/{projectId}/briefing/{sessionId}/questions — Get current questions.
   *
   * @param projectId UUID of project
   * @param sessionId UUID of briefing session
   * @return List of BriefingQuestionResponse
   */
  @GetMapping("/{sessionId}/questions")
  public ResponseEntity<List<BriefingQuestionResponse>> getQuestions(
      @PathVariable UUID projectId, @PathVariable UUID sessionId) {
    // TODO: Implement fetch questions logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/briefing/{sessionId}/answers — Submit answer to question.
   *
   * @param projectId UUID of project
   * @param sessionId UUID of briefing session
   * @param request SubmitAnswerRequest
   * @return BriefingAnswerResponse
   */
  @PostMapping("/{sessionId}/answers")
  public ResponseEntity<BriefingAnswerResponse> submitAnswer(
      @PathVariable UUID projectId,
      @PathVariable UUID sessionId,
      @RequestBody SubmitAnswerRequest request) {
    // TODO: Implement answer submission logic
    // Trigger AI analysis if needed (via RabbitMQ for async processing)
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * POST /projects/{projectId}/briefing/{sessionId}/complete — Complete briefing session.
   *
   * Triggers scope generation via AI.
   *
   * @param projectId UUID of project
   * @param sessionId UUID of briefing session
   * @return BriefingSessionResponse with status COMPLETED
   */
  @PostMapping("/{sessionId}/complete")
  public ResponseEntity<BriefingSessionResponse> completeBriefing(
      @PathVariable UUID projectId, @PathVariable UUID sessionId) {
    // TODO: Implement briefing completion logic
    // Trigger scope generation AI job (async via RabbitMQ)
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * DELETE /projects/{projectId}/briefing/{sessionId} — Discard briefing session.
   *
   * @param projectId UUID of project
   * @param sessionId UUID of briefing session
   * @return ResponseEntity with no content
   */
  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> discardBriefing(
      @PathVariable UUID projectId, @PathVariable UUID sessionId) {
    // TODO: Implement discard logic
    return ResponseEntity.noContent().build();
  }

  // ============================================================================
  // DTOs
  // ============================================================================

  public record CreateBriefingRequest(List<UUID> serviceIds) {}

  public record BriefingSessionResponse(
      UUID id,
      UUID projectId,
      String status,
      int currentStep,
      int totalSteps,
      String aiModel,
      double temperature) {}

  public record BriefingQuestionResponse(
      UUID id, int stepNumber, String questionText, String aiPromptVersion) {}

  public record SubmitAnswerRequest(UUID questionId, String answerText) {}

  public record BriefingAnswerResponse(
      UUID id, UUID questionId, boolean followupTriggered, String answeredAt) {}
}
