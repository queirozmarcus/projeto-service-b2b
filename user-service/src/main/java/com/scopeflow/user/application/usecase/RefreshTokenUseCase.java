package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.UserId;
import com.scopeflow.user.domain.model.User;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;

import java.util.Objects;
import java.util.UUID;

/**
 * Use case: validate a refresh token and issue a new access token.
 *
 * Invariants enforced:
 * - Token must be a valid refresh token (not access, not expired)
 * - User referenced by token must still exist and be active
 *
 * Zero Spring dependencies — pure Java orchestration.
 */
public class RefreshTokenUseCase {

    private final TokenIssuer tokenIssuer;
    private final UserRepository userRepository;

    public RefreshTokenUseCase(TokenIssuer tokenIssuer, UserRepository userRepository) {
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    public record Result(String accessToken, long expiresInSeconds) {}

    public Result execute(String refreshToken) {
        if (refreshToken == null || !tokenIssuer.isValidRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        UUID userId = tokenIssuer.extractUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(new UserId(userId))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        String newAccessToken = tokenIssuer.issueAccessToken(
                user.getId().value(), user.getEmail().normalized(), user.getWorkspaceId(), "USER");

        return new Result(newAccessToken, tokenIssuer.accessTokenExpirationSeconds());
    }
}
