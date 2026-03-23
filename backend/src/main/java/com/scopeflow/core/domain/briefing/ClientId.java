package com.scopeflow.core.domain.briefing;

import java.util.Objects;
import java.util.UUID;

/**
 * ClientId value object: wraps UUID for type safety.
 * Immutable, uses record semantics.
 */
public record ClientId(UUID value) {
    public ClientId {
        Objects.requireNonNull(value, "ClientId value cannot be null");
    }

    public static ClientId generate() {
        return new ClientId(UUID.randomUUID());
    }

    public static ClientId of(String uuidString) {
        return new ClientId(UUID.fromString(uuidString));
    }
}
