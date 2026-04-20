package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * Use case: invite a new (inactive) user into a workspace.
 *
 * Responsibilities:
 * - Generate temporary password and hash it
 * - Derive display name from email local part
 * - Create UserInactive via domain factory
 * - Persist via UserRepository port
 *
 * Duplicate email is enforced by the UNIQUE constraint on the email column.
 * DataIntegrityViolationException is caught in JpaUserRepositoryAdapter and converted
 * to EmailAlreadyRegisteredException, eliminating the check-then-act race condition.
 *
 * No Spring dependencies — wired via UseCaseConfig.
 */
public class InviteUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public InviteUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "PasswordHasher cannot be null");
    }

    /**
     * @param email       the invited user's email address (already validated as Email VO)
     * @param invitedBy   the UserId of the user who is sending the invite (validated by caller)
     * @return the persisted UserInactive
     */
    public UserInactive execute(Email email, UserId invitedBy) {
        Objects.requireNonNull(email, "Email cannot be null");
        Objects.requireNonNull(invitedBy, "InvitedBy cannot be null");

        String tempPassword = UUID.randomUUID().toString().substring(0, 12);
        PasswordHash tempHash = passwordHasher.hash(tempPassword);

        String displayName = extractDisplayName(email.normalized());

        UserId newUserId = UserId.generate();
        UserInactive invitedUser = User.createInvited(newUserId, email, tempHash, displayName);

        userRepository.save(invitedUser);

        return invitedUser;
    }

    /**
     * Converts the local part of an email into a display name.
     * E.g. "john.doe" → "John Doe", "maria_silva" → "Maria Silva"
     */
    private String extractDisplayName(String email) {
        String localPart = email.split("@")[0];
        return localPart.replace(".", " ").replace("_", " ")
                .chars()
                .collect(StringBuilder::new,
                        (sb, c) -> {
                            if (sb.isEmpty() || sb.charAt(sb.length() - 1) == ' ') {
                                sb.append((char) Character.toUpperCase(c));
                            } else {
                                sb.append((char) c);
                            }
                        },
                        StringBuilder::append)
                .toString();
    }
}
