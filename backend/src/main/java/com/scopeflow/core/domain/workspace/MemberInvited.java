package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Invited member state: invited but not yet accepted/confirmed.
 */
public final class MemberInvited extends WorkspaceMember {

    public MemberInvited(
            WorkspaceId workspaceId,
            UserId userId,
            Role role,
            Instant joinedAt,
            Instant updatedAt
    ) {
        super(workspaceId, userId, role, joinedAt, updatedAt);
    }

    @Override
    public String status() {
        return "INVITED";
    }
}
