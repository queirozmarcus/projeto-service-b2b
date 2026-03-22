package com.scopeflow.core.domain.briefing;

import java.util.Objects;
import java.util.UUID;

/**
 * BriefingSessionId value object: wraps UUID for type safety.
 * Immutable, uses record semantics.
 */
public record BriefingSessionId(UUID value) {
    public BriefingSessionId {
        Objects.requireNonNull(value, "BriefingSessionId value cannot be null");
    }

    public static BriefingSessionId generate() {
        return new BriefingSessionId(UUID.randomUUID());
    }

    public static BriefingSessionId of(String uuidString) {
        return new BriefingSessionId(UUID.fromString(uuidString));
    }
}
