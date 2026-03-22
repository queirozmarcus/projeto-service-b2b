package com.scopeflow.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ProposalController — Scope & Proposal Management.
 *
 * Endpoints: list proposals, get proposal, generate scope, publish proposal, render PDF.
 */
@RestController
@RequestMapping("/projects/{projectId}/proposals")
public class ProposalController {

  /**
   * GET /projects/{projectId}/proposals — List all proposals for project.
   *
   * @param projectId UUID of project
   * @param status optional filter by status (DRAFT, PUBLISHED, APPROVED, etc.)
   * @return List of ProposalResponse
   */
  @GetMapping
  public ResponseEntity<List<ProposalResponse>> listProposals(
      @PathVariable UUID projectId, @RequestParam(required = false) String status) {
    // TODO: Implement list proposals logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /projects/{projectId}/proposals/{proposalId} — Get proposal details.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return ProposalResponse
   */
  @GetMapping("/{proposalId}")
  public ResponseEntity<ProposalResponse> getProposal(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement fetch proposal logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/proposals/from-briefing/{briefingSessionId} — Generate proposal
   * from completed briefing.
   *
   * Triggers scope generation AI job (async via RabbitMQ).
   *
   * @param projectId UUID of project
   * @param briefingSessionId UUID of completed briefing session
   * @return ProposalResponse with status GENERATING
   */
  @PostMapping("/from-briefing/{briefingSessionId}")
  public ResponseEntity<ProposalResponse> generateProposalFromBriefing(
      @PathVariable UUID projectId, @PathVariable UUID briefingSessionId) {
    // TODO: Implement proposal generation logic
    // Trigger scope generation AI (async)
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * PUT /projects/{projectId}/proposals/{proposalId} — Update proposal (manual edit).
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @param request UpdateProposalRequest
   * @return ProposalResponse
   */
  @PutMapping("/{proposalId}")
  public ResponseEntity<ProposalResponse> updateProposal(
      @PathVariable UUID projectId,
      @PathVariable UUID proposalId,
      @RequestBody UpdateProposalRequest request) {
    // TODO: Implement proposal update logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/proposals/{proposalId}/publish — Publish proposal.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return ProposalResponse with status PUBLISHED
   */
  @PostMapping("/{proposalId}/publish")
  public ResponseEntity<ProposalResponse> publishProposal(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement publish logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /projects/{projectId}/proposals/{proposalId}/render-pdf — Render proposal as PDF.
   *
   * Triggers PDF generation (async via RabbitMQ).
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return URL to S3-hosted PDF or stream directly
   */
  @GetMapping("/{proposalId}/render-pdf")
  public ResponseEntity<ProposalPdfResponse> renderProposalPdf(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement PDF rendering logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /projects/{projectId}/proposals/{proposalId}/versions — Get proposal version history.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return List of ProposalVersionResponse
   */
  @GetMapping("/{proposalId}/versions")
  public ResponseEntity<List<ProposalVersionResponse>> getProposalVersions(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement version history logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * DELETE /projects/{projectId}/proposals/{proposalId} — Delete proposal.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return ResponseEntity with no content
   */
  @DeleteMapping("/{proposalId}")
  public ResponseEntity<Void> deleteProposal(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement delete logic
    return ResponseEntity.noContent().build();
  }

  // ============================================================================
  // DTOs
  // ============================================================================

  public record ProposalResponse(
      UUID id,
      String status,
      String proposalType,
      String title,
      String description,
      Object scope,
      Object deliverables,
      Object timeline,
      Double estimatedValue,
      String currency) {}

  public record UpdateProposalRequest(
      String title, String description, Object scope, Object deliverables, Double estimatedValue) {
  }

  public record ProposalPdfResponse(String pdfUrl, String s3Key, long fileSizeBytes) {}

  public record ProposalVersionResponse(
      UUID id,
      int versionNumber,
      String statusBefore,
      String statusAfter,
      Object changes,
      String changedAt) {}
}
