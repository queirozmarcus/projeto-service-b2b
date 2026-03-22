package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Domain event: Workspace member invited.
 * Published when WorkspaceMemberService.inviteMember() succeeds.
 */
public record WorkspaceMemberInvited(
        WorkspaceId workspaceId,
        UserId userId,
        Role role,
        Instant timestamp,
        String eventId
) {
    public WorkspaceMemberInvited(WorkspaceId workspaceId, UserId userId, Role role) {
        this(workspaceId, userId, role, Instant.now(), java.util.UUID.randomUUID().toString());
    }
}
