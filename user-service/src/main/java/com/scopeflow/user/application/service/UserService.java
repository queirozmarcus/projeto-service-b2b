package com.scopeflow.user.application.service;

import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * UserService: residual domain service for user queries and lifecycle operations
 * not yet migrated to dedicated use cases.
 *
 * registerUser() removed in S7 — fully replaced by RegisterUserUseCase (S6).
 * saveInvitedUser() retained for compatibility; logic now lives in InviteUserUseCase (S7).
 */
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
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
