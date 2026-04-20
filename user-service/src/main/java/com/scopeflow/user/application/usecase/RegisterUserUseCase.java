package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;

/**
 * Use case: register a new user account.
 *
 * Invariants enforced:
 * - Email must be unique across system
 * - Password is hashed by the port (never stored plain)
 *
 * Zero Spring dependencies — pure Java orchestration.
 */
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserUseCase(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
    }

    public UserActive execute(String rawEmail, String rawPassword, String fullName, String phone) {
        Email email = new Email(rawEmail);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email already registered: " + email.normalized());
        }

        PasswordHash hash = new PasswordHash(passwordHasher.hash(rawPassword));
        UserId userId = UserId.generate();
        UserActive user = User.create(userId, email, hash, fullName, phone);
        userRepository.save(user);
        return user;
    }
}
