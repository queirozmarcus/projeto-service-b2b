package com.scopeflow.core.domain.user;

import java.time.Instant;

/**
 * Domain event: User registered (created a new account).
 * Published when UserActive is created via UserService.registerUser().
 *
 * Immutable record for event sourcing.
 */
public record UserRegistered(
        UserId userId,
        Email email,
        String fullName,
        Instant timestamp,
        String eventId
) {
    public UserRegistered(UserId userId, Email email, String fullName) {
        this(userId, email, fullName, Instant.now(), java.util.UUID.randomUUID().toString());
    }
}
