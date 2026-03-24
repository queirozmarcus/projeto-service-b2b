package com.scopeflow.adapter.out.persistence.proposal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for Proposal aggregate root.
 * Maps to 'proposals' table.
 *
 * Scope is stored via ProposalVersion (JSONB snapshot) — not duplicated here.
 */
@Entity
@Table(
        name = "proposals",
        indexes = {
                @Index(name = "idx_proposals_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_proposals_client_id", columnList = "client_id"),
                @Index(name = "idx_proposals_briefing_id", columnList = "briefing_id"),
                @Index(name = "idx_proposals_status", columnList = "status")
        }
)
public class JpaProposal {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "briefing_id", nullable = false)
    private UUID briefingId;

    @Column(name = "proposal_name", nullable = false, length = 500)
    private String proposalName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    // Scope stored as JSONB snapshot in current state
    @Column(name = "scope_json", columnDefinition = "jsonb")
    private String scopeJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected JpaProposal() {
        // JPA only
    }

    public JpaProposal(
            UUID id,
            UUID workspaceId,
            UUID clientId,
            UUID briefingId,
            String proposalName,
            String status,
            String scopeJson,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.workspaceId = Objects.requireNonNull(workspaceId);
        this.clientId = Objects.requireNonNull(clientId);
        this.briefingId = Objects.requireNonNull(briefingId);
        this.proposalName = Objects.requireNonNull(proposalName);
        this.status = Objects.requireNonNull(status);
        this.scopeJson = scopeJson;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public void setStatus(String status) { this.status = status; }
    public void setScopeJson(String scopeJson) { this.scopeJson = scopeJson; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public UUID getId() { return id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public UUID getClientId() { return clientId; }
    public UUID getBriefingId() { return briefingId; }
    public String getProposalName() { return proposalName; }
    public String getStatus() { return status; }
    public String getScopeJson() { return scopeJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaProposal that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
