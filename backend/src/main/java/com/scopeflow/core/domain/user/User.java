package com.scopeflow.core.domain.user;

import java.time.Instant;
import java.util.Objects;

/**
 * User aggregate root (sealed class for type safety).
 *
 * States:
 * - UserActive: can login, active session
 * - UserInactive: invited but not confirmed email
 * - UserDeleted: soft-deleted (GDPR compliance)
 *
 * No framework dependencies. Pure domain logic.
 */
public sealed class User permits UserActive, UserInactive, UserDeleted {
    private final UserId id;
    private final Email email;
    private final PasswordHash passwordHash;
    private final String fullName;
    private final String phone;
    private final Instant createdAt;
    private final Instant updatedAt;

    protected User(
            UserId id,
            Email email,
            PasswordHash passwordHash,
            String fullName,
            String phone,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "UserId cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "PasswordHash cannot be null");
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null");
        this.phone = phone; // optional
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "UpdatedAt cannot be null");
    }

    /**
     * Factory method: create a new active user.
     */
    public static UserActive create(
            UserId id,
            Email email,
            PasswordHash passwordHash,
            String fullName,
            String phone
    ) {
        return new UserActive(id, email, passwordHash, fullName, phone, Instant.now(), Instant.now());
    }

    /**
     * Factory method: create an invited (inactive) user.
     *
     * Used when inviting a user by email who does not yet have an account.
     * User remains INACTIVE until they accept the invite and set their password.
     */
    public static UserInactive createInvited(
            UserId id,
            Email email,
            PasswordHash temporaryPasswordHash,
            String fullName
    ) {
        return new UserInactive(id, email, temporaryPasswordHash, fullName, null, Instant.now(), Instant.now());
    }

    // ============ Accessors ============

    public UserId getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public PasswordHash getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ============ Abstract Methods ============

    /**
     * Returns the status of this user (for pattern matching).
     */
    public abstract String status();

    /**
     * Check if user can login.
     */
    public abstract boolean canLogin();

    // ============ Value-based equals & hashCode ============

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email=" + email +
                ", status=" + status() +
                ", fullName='" + fullName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
