package com.scopeflow.adapter.out.persistence.proposal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for ApprovalWorkflow.
 * Maps to 'approval_workflows' table.
 */
@Entity
@Table(
        name = "approval_workflows",
        indexes = {
                @Index(name = "idx_approval_workflows_proposal_id", columnList = "proposal_id"),
                @Index(name = "idx_approval_workflows_status", columnList = "status")
        }
)
public class JpaApprovalWorkflow {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false)
    private UUID proposalId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "initiated_at", nullable = false, updatable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected JpaApprovalWorkflow() {
        // JPA only
    }

    public JpaApprovalWorkflow(
            UUID id,
            UUID proposalId,
            String status,
            Instant initiatedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.proposalId = Objects.requireNonNull(proposalId);
        this.status = Objects.requireNonNull(status);
        this.initiatedAt = Objects.requireNonNull(initiatedAt);
        this.completedAt = completedAt;
    }

    public void setStatus(String status) { this.status = status; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public String getStatus() { return status; }
    public Instant getInitiatedAt() { return initiatedAt; }
    public Instant getCompletedAt() { return completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaApprovalWorkflow that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
