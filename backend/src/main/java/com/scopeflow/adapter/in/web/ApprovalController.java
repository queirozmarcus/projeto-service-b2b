package com.scopeflow.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ApprovalController — Proposal Approval Workflows.
 *
 * Endpoints: start approval workflow, get approvals, approve/reject, get status, initiate kickoff.
 */
@RestController
@RequestMapping("/projects/{projectId}/proposals/{proposalId}/approvals")
public class ApprovalController {

  /**
   * POST /projects/{projectId}/proposals/{proposalId}/approvals — Start approval workflow.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @param request InitiateApprovalRequest with list of required approvers
   * @return ApprovalWorkflowResponse
   */
  @PostMapping
  public ResponseEntity<ApprovalWorkflowResponse> initiateApprovalWorkflow(
      @PathVariable UUID projectId,
      @PathVariable UUID proposalId,
      @RequestBody InitiateApprovalRequest request) {
    // TODO: Implement approval workflow initiation logic
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * GET /projects/{projectId}/proposals/{proposalId}/approvals — Get approval workflow status.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return ApprovalWorkflowResponse
   */
  @GetMapping
  public ResponseEntity<ApprovalWorkflowResponse> getApprovalWorkflow(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement fetch workflow logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /projects/{projectId}/proposals/{proposalId}/approvals/approvers — List pending
   * approvals.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return List of ApprovalResponse (status: PENDING, APPROVED, REJECTED)
   */
  @GetMapping("/approvers")
  public ResponseEntity<List<ApprovalResponse>> listApprovals(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement list approvals logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/proposals/{proposalId}/approvals/{approvalId}/approve — Approve
   * proposal.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @param approvalId UUID of approval task
   * @param request ApproveRequest with optional comments
   * @return ApprovalResponse with status APPROVED
   */
  @PostMapping("/{approvalId}/approve")
  public ResponseEntity<ApprovalResponse> approveProposal(
      @PathVariable UUID projectId,
      @PathVariable UUID proposalId,
      @PathVariable UUID approvalId,
      @RequestBody ApproveRequest request) {
    // TODO: Implement approval logic
    // Check if all approvals complete → trigger kickoff generation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/proposals/{proposalId}/approvals/{approvalId}/reject — Reject
   * proposal.
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @param approvalId UUID of approval task
   * @param request RejectRequest with rejection reason
   * @return ApprovalResponse with status REJECTED
   */
  @PostMapping("/{approvalId}/reject")
  public ResponseEntity<ApprovalResponse> rejectProposal(
      @PathVariable UUID projectId,
      @PathVariable UUID proposalId,
      @PathVariable UUID approvalId,
      @RequestBody RejectRequest request) {
    // TODO: Implement rejection logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /projects/{projectId}/proposals/{proposalId}/approvals/complete — Trigger kickoff
   * summary generation.
   *
   * Called after all approvals are complete. Generates kickoff summary via AI (async RabbitMQ).
   *
   * @param projectId UUID of project
   * @param proposalId UUID of proposal
   * @return KickoffSummaryResponse
   */
  @PostMapping("/complete")
  public ResponseEntity<KickoffSummaryResponse> completeAndGenerateKickoff(
      @PathVariable UUID projectId, @PathVariable UUID proposalId) {
    // TODO: Implement kickoff generation logic
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  // ============================================================================
  // DTOs
  // ============================================================================

  public record InitiateApprovalRequest(List<UUID> approverIds) {}

  public record ApprovalWorkflowResponse(
      UUID id,
      UUID proposalId,
      String status,
      int requiredApprovals,
      int currentApprovals,
      String createdAt,
      String completedAt) {}

  public record ApprovalResponse(
      UUID id,
      UUID approverId,
      String approverName,
      String approverEmail,
      String status,
      String approvedAt,
      String comments) {}

  public record ApproveRequest(String comments) {}

  public record RejectRequest(String rejectionReason, String comments) {}

  public record KickoffSummaryResponse(
      UUID id,
      UUID proposalId,
      String content,
      Object keyMilestones,
      Object nextSteps,
      Object stakeholders,
      String createdAt) {}
}
