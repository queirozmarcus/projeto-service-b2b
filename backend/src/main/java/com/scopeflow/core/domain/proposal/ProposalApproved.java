package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.UUID;

/**
 * Approved proposal: client approved.
 */
public final class ProposalApproved extends Proposal {

    public ProposalApproved(
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
        return ProposalStatus.APPROVED;
    }
}
