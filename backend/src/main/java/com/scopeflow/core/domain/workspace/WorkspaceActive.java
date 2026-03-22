package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.time.Instant;

/**
 * Active workspace state: normal operation.
 */
public final class WorkspaceActive extends Workspace {

    public WorkspaceActive(
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
        return "ACTIVE";
    }
}
