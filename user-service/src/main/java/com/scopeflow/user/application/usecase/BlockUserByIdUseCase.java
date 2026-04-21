package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.UserId;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Use case: blocks a user by changing their status to BLOCKED.
 *
 * Blocked users cannot login. This is a permanent state change (unlike the
 * temporary JWT blocklist handled by BlockUserUseCase).
 *
 * This operation is idempotent — blocking an already blocked user succeeds.
 *
 * Authorization: Only ADMIN role can execute this use case.
 * Controller layer must enforce @PreAuthorize("hasRole('ADMIN')").
 */
@Service
public class BlockUserByIdUseCase {

    private static final Logger log = LoggerFactory.getLogger(BlockUserByIdUseCase.class);

    private final UserRepository userRepository;

    public BlockUserByIdUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
    }

    /**
     * Blocks the user with the given ID.
     *
     * @param userId the user ID to block
     * @throws UserNotFoundException if user does not exist
     */
    public void execute(UserId userId) {
        Objects.requireNonNull(userId, "UserId cannot be null");

        // Validate user exists
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Check if already blocked (idempotent operation)
        if ("BLOCKED".equals(user.status())) {
            log.info("User already blocked: userId={}", userId.value());
            return;
        }

        // Block user
        userRepository.blockUser(userId);

        log.info("User blocked successfully: userId={}, previousStatus={}", userId.value(), user.status());
    }
}
