package com.scopeflow.core.domain.user;

import java.time.Instant;

/**
 * Active user state: can login, authenticated, fully registered.
 */
public final class UserActive extends User {

    public UserActive(
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
        return "ACTIVE";
    }

    @Override
    public boolean canLogin() {
        return true;
    }
}
