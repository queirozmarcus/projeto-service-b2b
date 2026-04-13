package com.scopeflow.core.domain.proposal;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Approval: a single approver's decision on a proposal.
 *
 * Immutable once approved (insert-only semantics for audit).
 */
public record Approval(
        UUID id,
        ApprovalWorkflowId workflowId,
        String approverName,
        String approverEmail,
        ApprovalStatus status,
        String ipAddress,
        String userAgent,
        Instant approvedAt
) {
    public Approval {
        Objects.requireNonNull(id, "Approval id cannot be null");
        Objects.requireNonNull(workflowId, "WorkflowId cannot be null");
        Objects.requireNonNull(approverEmail, "ApproverEmail cannot be null");
        Objects.requireNonNull(status, "Status cannot be null");
    }

    /**
     * Creates a pending approval for an approver email (before they act).
     */
    public static Approval createPending(UUID id, String approverEmail) {
        return new Approval(id, null, null, approverEmail, ApprovalStatus.PENDING,
                null, null, null);
    }
}
