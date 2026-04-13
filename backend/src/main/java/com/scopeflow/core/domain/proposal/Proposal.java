package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Proposal aggregate root (sealed class).
 *
 * States:
 * - ProposalDraft: being edited, not visible to client
 * - ProposalPublished: shared with client, awaiting approval
 * - ProposalApproved: client approved
 * - ProposalRejected: client rejected
 *
 * No framework dependencies. Pure domain.
 */
public abstract sealed class Proposal permits ProposalDraft, ProposalPublished, ProposalApproved, ProposalRejected {

    private final ProposalId id;
    private final WorkspaceId workspaceId;
    private final UUID clientId;
    private final BriefingSessionId briefingId;
    private final String proposalName;
    private final ProposalScope scope;
    private final Instant createdAt;
    private final Instant updatedAt;

    protected Proposal(
            ProposalId id,
            WorkspaceId workspaceId,
            UUID clientId,
            BriefingSessionId briefingId,
            String proposalName,
            ProposalScope scope,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "ProposalId cannot be null");
        this.workspaceId = Objects.requireNonNull(workspaceId, "WorkspaceId cannot be null");
        this.clientId = Objects.requireNonNull(clientId, "ClientId cannot be null");
        this.briefingId = Objects.requireNonNull(briefingId, "BriefingId cannot be null");
        this.proposalName = Objects.requireNonNull(proposalName, "ProposalName cannot be null");
        this.scope = scope; // nullable during creation
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    /**
     * Factory: create a new draft proposal.
     */
    public static ProposalDraft create(
            WorkspaceId workspaceId,
            UUID clientId,
            BriefingSessionId briefingId,
            String proposalName
    ) {
        return new ProposalDraft(
                ProposalId.generate(),
                workspaceId,
                clientId,
                briefingId,
                proposalName,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    // ============ Accessors ============

    public ProposalId getId() { return id; }
    public WorkspaceId getWorkspaceId() { return workspaceId; }
    public UUID getClientId() { return clientId; }
    public BriefingSessionId getBriefingId() { return briefingId; }
    public String getProposalName() { return proposalName; }
    public ProposalScope getScope() { return scope; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public abstract ProposalStatus status();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Proposal)) return false;
        return Objects.equals(id, ((Proposal) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Proposal{id=" + id + ", status=" + status() + ", name='" + proposalName + "'}";
    }
}
