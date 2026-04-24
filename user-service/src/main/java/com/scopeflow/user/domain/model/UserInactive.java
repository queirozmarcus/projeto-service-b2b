package com.scopeflow.user.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Inactive user state: invited but not yet confirmed email.
 */
public final class UserInactive extends User {

    public UserInactive(
            UserId id, Email email, PasswordHash passwordHash,
            String fullName, String phone, UUID workspaceId, Instant createdAt, Instant updatedAt
    ) {
        super(id, email, passwordHash, fullName, phone, workspaceId, createdAt, updatedAt);
    }

    @Override
    public User withWorkspace(UUID workspaceId) {
        java.util.Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        return new UserInactive(getId(), getEmail(), getPasswordHash(), getFullName(), getPhone(),
                workspaceId, getCreatedAt(), java.time.Instant.now());
    }

    @Override
    public String status() { return "INACTIVE"; }

    @Override
    public boolean canLogin() { return false; }
}
