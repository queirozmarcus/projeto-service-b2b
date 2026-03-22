package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Left member state: removed/exited workspace (historical record).
 */
public final class MemberLeft extends WorkspaceMember {

    public MemberLeft(
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
        return "LEFT";
    }
}
