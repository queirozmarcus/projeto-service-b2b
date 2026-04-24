package com.scopeflow.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * UserBlocked state: user has been blocked by an admin.
 *
 * Blocked users cannot login. This is a permanent state (unlike temporary
 * JWT blocklist entries).
 */
public final class UserBlocked extends User {

    public UserBlocked(
            UserId id,
            Email email,
            PasswordHash passwordHash,
            String fullName,
            String phone,
            UUID workspaceId,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, email, passwordHash, fullName, phone, workspaceId, createdAt, updatedAt);
    }

    @Override
    public User withWorkspace(UUID workspaceId) {
        throw new com.scopeflow.user.domain.exception.UserStateException(getId(), status(), "withWorkspace");
    }

    @Override
    public String status() {
        return "BLOCKED";
    }

    @Override
    public boolean canLogin() {
        return false;
    }
}
