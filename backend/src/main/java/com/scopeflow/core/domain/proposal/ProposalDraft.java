package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.UUID;

/**
 * Draft proposal: being edited, not visible to client.
 */
public final class ProposalDraft extends Proposal {

    public ProposalDraft(
            ProposalId id,
            WorkspaceId workspaceId,
            UUID clientId,
            BriefingSessionId briefingId,
            String proposalName,
            ProposalScope scope,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, workspaceId, clientId, briefingId, proposalName, scope, createdAt, updatedAt);
    }

    @Override
    public ProposalStatus status() {
        return ProposalStatus.DRAFT;
    }

    /**
     * Publish this proposal: transitions to ProposalPublished.
     *
     * @throws InvalidProposalStateException if scope is missing
     */
    public ProposalPublished publish() {
        if (getScope() == null) {
            throw new InvalidProposalStateException("Cannot publish proposal without a scope defined");
        }
        return new ProposalPublished(
                getId(), getWorkspaceId(), getClientId(), getBriefingId(),
                getProposalName(), getScope(), getCreatedAt(), Instant.now()
        );
    }

    /**
     * Update scope (creates new draft with updated scope).
     */
    public ProposalDraft updateScope(ProposalScope newScope) {
        return new ProposalDraft(
                getId(), getWorkspaceId(), getClientId(), getBriefingId(),
                getProposalName(), newScope, getCreatedAt(), Instant.now()
        );
    }

    /**
     * Rename proposal (creates new draft with updated name).
     *
     * @throws IllegalArgumentException if name is blank
     */
    public ProposalDraft rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Proposal name cannot be blank");
        }
        return new ProposalDraft(
                getId(), getWorkspaceId(), getClientId(), getBriefingId(),
                newName.strip(), getScope(), getCreatedAt(), Instant.now()
        );
    }
}
