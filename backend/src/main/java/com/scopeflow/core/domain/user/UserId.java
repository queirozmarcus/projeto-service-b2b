package com.scopeflow.core.domain.user;

import java.util.Objects;
import java.util.UUID;

/**
 * UserId value object: wraps UUID for type safety.
 * Immutable, uses record semantics.
 */
public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId value cannot be null");
    }

    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(String uuidString) {
        return new UserId(UUID.fromString(uuidString));
    }
}
