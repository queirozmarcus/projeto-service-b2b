package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.time.Instant;
import java.util.UUID;

/**
 * Rejected proposal: client rejected.
 */
public final class ProposalRejected extends Proposal {

    public ProposalRejected(
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
        return ProposalStatus.REJECTED;
    }
}
