package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.UUID;

/**
 * Published proposal: shared with client via approval link.
 */
public final class ProposalPublished extends Proposal {

    public ProposalPublished(
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
        return ProposalStatus.PUBLISHED;
    }

    /**
     * Approve this proposal.
     */
    public ProposalApproved approve() {
        return new ProposalApproved(
                getId(), getWorkspaceId(), getClientId(), getBriefingId(),
                getProposalName(), getScope(), getCreatedAt(), Instant.now()
        );
    }

    /**
     * Reject this proposal.
     */
    public ProposalRejected reject() {
        return new ProposalRejected(
                getId(), getWorkspaceId(), getClientId(), getBriefingId(),
                getProposalName(), getScope(), getCreatedAt(), Instant.now()
        );
    }
}
