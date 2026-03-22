package com.scopeflow.core.domain.user;

import java.util.Optional;

/**
 * UserRepository interface (domain layer, port).
 *
 * No @Repository annotation here — this is a port (domain interface).
 * Implementations are in adapter layer (adapter/out/persistence/).
 *
 * Repositories define the query contract; domain doesn't know about JPA.
 */
public interface UserRepository {

    /**
     * Save a user (create or update).
     */
    void save(User user);

    /**
     * Find user by ID.
     */
    Optional<User> findById(UserId id);

    /**
     * Find user by email (case-insensitive).
     */
    Optional<User> findByEmail(Email email);

    /**
     * Check if email already exists.
     */
    boolean existsByEmail(Email email);

    /**
     * Delete a user (soft-delete, changes status to DELETED).
     */
    void delete(UserId id);
}
