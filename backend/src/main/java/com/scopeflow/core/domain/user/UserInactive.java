package com.scopeflow.core.domain.user;

import java.time.Instant;

/**
 * Inactive user state: invited but not yet confirmed email.
 */
public final class UserInactive extends User {

    public UserInactive(
            UserId id,
            Email email,
            PasswordHash passwordHash,
            String fullName,
            String phone,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, email, passwordHash, fullName, phone, createdAt, updatedAt);
    }

    @Override
    public String status() {
        return "INACTIVE";
    }

    @Override
    public boolean canLogin() {
        return false; // Cannot login until email confirmed
    }
}
