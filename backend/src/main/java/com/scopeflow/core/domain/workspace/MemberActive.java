package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Active member state: currently a member of workspace.
 */
public final class MemberActive extends WorkspaceMember {

    public MemberActive(
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
        return "ACTIVE";
    }
}
