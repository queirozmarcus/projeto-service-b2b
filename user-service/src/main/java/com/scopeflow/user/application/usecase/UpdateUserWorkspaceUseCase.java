package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.exception.UserStateException;
import com.scopeflow.user.domain.exception.WorkspaceAlreadyAssignedException;
import com.scopeflow.user.domain.model.User;
import com.scopeflow.user.domain.model.UserActive;
import com.scopeflow.user.domain.model.UserInactive;
import com.scopeflow.user.domain.model.UserId;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * Use case: assign a workspace to an existing user.
 *
 * Called by the monolith after workspace creation (Strangler Fig flow).
 *
 * Invariants enforced:
 * - User must exist
 * - workspaceId must not be null
 * - User must be in state UserActive or UserInactive (not Blocked/Deleted)
 * - User must not already have a different workspaceId assigned
 *
 * Zero Spring dependencies — pure Java orchestration.
 */
public class UpdateUserWorkspaceUseCase {

    private final UserRepository userRepository;

    public UpdateUserWorkspaceUseCase(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public User execute(UserId userId, UUID workspaceId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // Only UserActive and UserInactive may receive a workspace assignment
        if (!(user instanceof UserActive || user instanceof UserInactive)) {
            throw new UserStateException(userId, user.status(), "assign workspace");
        }

        // Idempotent: same workspaceId is a no-op
        if (workspaceId.equals(user.getWorkspaceId())) {
            return user;
        }

        // Different workspaceId already set — reject to prevent accidental re-assignment
        if (user.getWorkspaceId() != null) {
            throw new WorkspaceAlreadyAssignedException(userId, user.getWorkspaceId());
        }

        User updated = user.withWorkspace(workspaceId);
        userRepository.save(updated);
        return updated;
    }
}
