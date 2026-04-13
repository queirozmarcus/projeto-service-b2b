package com.scopeflow.adapter.out.persistence.proposal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity for ProposalVersion.
 * Maps to 'proposal_versions' table.
 *
 * Immutable: only inserts, never updates (preserves history).
 * Scope is serialized as JSONB.
 */
@Entity
@Table(
        name = "proposal_versions",
        indexes = {
                @Index(name = "idx_proposal_versions_proposal_id", columnList = "proposal_id"),
                @Index(name = "idx_proposal_versions_created_at", columnList = "created_at")
        }
)
public class JpaProposalVersion {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "proposal_id", nullable = false, updatable = false)
    private UUID proposalId;

    @Column(name = "scope_json", nullable = false, columnDefinition = "jsonb", updatable = false)
    private String scopeJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    protected JpaProposalVersion() {
        // JPA only
    }

    public JpaProposalVersion(
            UUID id,
            UUID proposalId,
            String scopeJson,
            Instant createdAt,
            UUID createdBy
    ) {
        this.id = Objects.requireNonNull(id);
        this.proposalId = Objects.requireNonNull(proposalId);
        this.scopeJson = Objects.requireNonNull(scopeJson);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.createdBy = Objects.requireNonNull(createdBy);
    }

    public UUID getId() { return id; }
    public UUID getProposalId() { return proposalId; }
    public String getScopeJson() { return scopeJson; }
    public Instant getCreatedAt() { return createdAt; }
    public UUID getCreatedBy() { return createdBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpaProposalVersion that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
