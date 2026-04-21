package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.Email;
import com.scopeflow.user.domain.model.User;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;

/**
 * Use case: authenticate a user and issue tokens.
 *
 * Invariants enforced:
 * - User must exist and have an active account (canLogin)
 * - Password must match the stored hash
 *
 * Zero Spring dependencies — pure Java orchestration.
 */
public class AuthenticateUserUseCase {

    public record AuthTokens(String accessToken, String refreshToken) {}

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AuthenticateUserUseCase(UserRepository userRepository,
                                   PasswordHasher passwordHasher,
                                   TokenIssuer tokenIssuer) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
    }

    public record Result(User user, AuthTokens tokens) {}

    public Result execute(String rawEmail, String rawPassword) {
        Email email = new Email(rawEmail);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash().value())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        String accessToken = tokenIssuer.issueAccessToken(
                user.getId().value(), user.getEmail().normalized(), user.getWorkspaceId(), "USER");
        String refreshToken = tokenIssuer.issueRefreshToken(user.getId().value());

        return new Result(user, new AuthTokens(accessToken, refreshToken));
    }
}
