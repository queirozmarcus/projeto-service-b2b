package com.scopeflow.user.application.service;

import com.scopeflow.user.application.usecase.BlockUserUseCase;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.time.Duration;
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
    private final BlockUserUseCase blockUserUseCase;
    private final Duration accessTokenTtl;

    public UserService(UserRepository userRepository,
                       BlockUserUseCase blockUserUseCase,
                       Duration accessTokenTtl) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.blockUserUseCase = Objects.requireNonNull(blockUserUseCase, "BlockUserUseCase cannot be null");
        this.accessTokenTtl = Objects.requireNonNull(accessTokenTtl, "accessTokenTtl cannot be null");
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

    /**
     * Deletes the user and adds their userId to the blocklist.
     *
     * The block TTL matches the access token lifetime so any in-flight JWT
     * issued before deletion is rejected until it naturally expires.
     */
    public void deactivateUser(UserId userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.delete(userId);
        blockUserUseCase.execute(userId.value().toString(), accessTokenTtl);
    }
}
