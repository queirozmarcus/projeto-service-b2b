package com.scopeflow.core.domain.workspace;

import java.util.Objects;
import java.util.UUID;

/**
 * WorkspaceId value object: wraps UUID for type safety.
 */
public record WorkspaceId(UUID value) {
    public WorkspaceId {
        Objects.requireNonNull(value, "WorkspaceId value cannot be null");
    }

    public static WorkspaceId generate() {
        return new WorkspaceId(UUID.randomUUID());
    }

    public static WorkspaceId of(String uuidString) {
        return new WorkspaceId(UUID.fromString(uuidString));
    }
}
