package com.scopeflow.core.domain.proposal;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ApprovalWorkflow aggregate: tracks the approval process for a proposal.
 *
 * Each workflow has one or more Approval records (one per approver).
 * Workflow is complete when all approvals are resolved.
 */
public record ApprovalWorkflow(
        ApprovalWorkflowId id,
        ProposalId proposalId,
        ApprovalStatus status,
        List<Approval> approvals,
        Instant initiatedAt,
        Instant completedAt
) {
    public ApprovalWorkflow {
        Objects.requireNonNull(id, "ApprovalWorkflowId cannot be null");
        Objects.requireNonNull(proposalId, "ProposalId cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
        approvals = approvals != null ? List.copyOf(approvals) : List.of();
        Objects.requireNonNull(initiatedAt, "InitiatedAt cannot be null");
    }

    public static ApprovalWorkflow create(ProposalId proposalId, List<String> approverEmails) {
        List<Approval> approvals = approverEmails.stream()
                .map(email -> Approval.createPending(UUID.randomUUID(), email))
                .toList();
        return new ApprovalWorkflow(
                ApprovalWorkflowId.generate(),
                proposalId,
                ApprovalStatus.IN_PROGRESS,
                approvals,
                Instant.now(),
                null
        );
    }
}
