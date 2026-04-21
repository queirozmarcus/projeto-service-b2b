package com.scopeflow.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Active user state: can login, authenticated, fully registered.
 */
public final class UserActive extends User {

    public UserActive(
            UserId id, Email email, PasswordHash passwordHash,
            String fullName, String phone, UUID workspaceId, Instant createdAt, Instant updatedAt
    ) {
        super(id, email, passwordHash, fullName, phone, workspaceId, createdAt, updatedAt);
    }

    @Override
    public String status() { return "ACTIVE"; }

    @Override
    public boolean canLogin() { return true; }
}
