package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Suspended workspace state: owner paused subscription.
 */
public final class WorkspaceSuspended extends Workspace {

    public WorkspaceSuspended(
            WorkspaceId id,
            UserId ownerId,
            String name,
            String niche,
            String toneSettings,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, ownerId, name, niche, toneSettings, createdAt, updatedAt);
    }

    @Override
    public String status() {
        return "SUSPENDED";
    }
}
