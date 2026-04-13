package com.scopeflow.adapter.out.persistence.proposal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Approval.
 * Maps to 'approvals' table.
 *
 * Immutable once created (audit trail). Status transitions via new inserts or soft updates.
 */
@Entity
@Table(
        name = "approvals",
        indexes = {
                @Index(name = "idx_approvals_workflow_id", columnList = "workflow_id"),
                @Index(name = "idx_approvals_status", columnList = "status"),
                @Index(name = "idx_approvals_approver_email", columnList = "approver_email")
        }
)
public class JpaApproval {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "approver_name", length = 255)
    private String approverName;

    @Column(name = "approver_email", nullable = false, length = 255)
    private String approverEmail;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "approved_at")
    private Instant approvedAt;

    protected JpaApproval() {
        // JPA only
    }

    public JpaApproval(
            UUID id,
            UUID workflowId,
            String approverName,
            String approverEmail,
            String status,
            String ipAddress,
            String userAgent,
            Instant approvedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.workflowId = Objects.requireNonNull(workflowId);
        this.approverName = approverName;
        this.approverEmail = Objects.requireNonNull(approverEmail);
        this.status = Objects.requireNonNull(status);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.approvedAt = approvedAt;
    }

    public void setApproverName(String approverName) { this.approverName = approverName; }
    public void setStatus(String status) { this.status = status; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getApproverName() { return approverName; }
    public String getApproverEmail() { return approverEmail; }
    public String getStatus() { return status; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getApprovedAt() { return approvedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaApproval that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
