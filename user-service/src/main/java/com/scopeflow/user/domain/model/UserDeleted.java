package com.scopeflow.user.domain.model;

import java.time.Instant;

/**
 * Deleted user state: soft-deleted (GDPR compliance).
 */
public final class UserDeleted extends User {

    public UserDeleted(
            UserId id, Email email, PasswordHash passwordHash,
            String fullName, String phone, Instant createdAt, Instant updatedAt
    ) {
        super(id, email, passwordHash, fullName, phone, createdAt, updatedAt);
    }

    @Override
    public String status() { return "DELETED"; }

    @Override
    public boolean canLogin() { return false; }
}
