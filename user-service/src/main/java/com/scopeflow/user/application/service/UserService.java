package com.scopeflow.user.application.service;

import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.Optional; // usado em getUserById / getUserByEmail

/**
 * UserService: domain service for user lifecycle.
 *
 * Contains business logic (invariants, workflows).
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
     * @deprecated Replaced by RegisterUserUseCase (S6). TODO: remover após S6.
     */
    @Deprecated
    public UserActive registerUser(Email email, PasswordHash passwordHash, String fullName, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered: " + email.normalized());
        }

        UserId userId = UserId.generate();
        UserActive user = User.create(userId, email, passwordHash, fullName, phone);
        userRepository.save(user);
        return user;
    }

    public Optional<User> getUserById(UserId userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByEmail(Email email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Persist an invited (inactive) user created by the workspace invite flow.
     */
    public void saveInvitedUser(UserInactive user) {
        Objects.requireNonNull(user, "Invited user cannot be null");
        userRepository.save(user);
    }

    public void deactivateUser(UserId userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(userId);
    }
}
