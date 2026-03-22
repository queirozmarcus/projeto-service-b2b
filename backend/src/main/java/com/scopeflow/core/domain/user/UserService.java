package com.scopeflow.core.domain.user;

import java.util.Objects;
import java.util.Optional;

/**
 * UserService: domain service for user lifecycle.
 *
 * Contains business logic (invariants, workflows).
 * No @Service annotation here — this is a domain service.
 * Spring @Service wrappers are in application layer (use cases).
 *
 * Invariants:
 * - Email must be unique across system
 * - Password must be hashed (never plaintext in domain)
 * - User can only be in one state (sealed class enforces)
 */
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
    }

    /**
     * Register a new user.
     *
     * Invariant: Email must be unique.
     * Returns: UserActive (ready to login).
     *
     * @param email validated email
     * @param passwordHash already-hashed password
     * @param fullName user's full name
     * @param phone optional phone number
     * @return newly created UserActive
     * @throws EmailAlreadyRegisteredException if email exists
     */
    public UserActive registerUser(
            Email email,
            PasswordHash passwordHash,
            String fullName,
            String phone
    ) {
        // Enforce invariant: email must be unique
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered: " + email.normalized());
        }

        // Create new user
        UserId userId = UserId.generate();
        UserActive user = User.create(userId, email, passwordHash, fullName, phone);

        // Persist
        userRepository.save(user);

        // Note: In adapter layer, UserService would be wrapped with @Service
        // and eventPublisher.publish(new UserRegistered(...)) would occur here.
        // We keep domain service pure (no event publishing here).

        return user;
    }

    /**
     * Get user by ID.
     */
    public Optional<User> getUserById(UserId userId) {
        return userRepository.findById(userId);
    }

    /**
     * Get user by email.
     */
    public Optional<User> getUserByEmail(Email email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Deactivate a user (soft delete).
     */
    public void deactivateUser(UserId userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            // In real implementation, would transition to UserDeleted state
            // and call userRepository.save(new UserDeleted(...))
            userRepository.delete(userId);
        }
    }
}
