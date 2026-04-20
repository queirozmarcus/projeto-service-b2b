package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenUseCase")
class RefreshTokenUseCaseTest {

    @Mock private TokenIssuer tokenIssuer;
    @Mock private UserRepository userRepository;

    private RefreshTokenUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final Email email = new Email("user@example.com");
    private final PasswordHash hash = new PasswordHash("$2a$12$hashed");

    @BeforeEach
    void setUp() {
        useCase = new RefreshTokenUseCase(tokenIssuer, userRepository);
    }

    @Test
    @DisplayName("should return new access token when refresh token is valid")
    void shouldReturnNewAccessToken_whenRefreshTokenIsValid() {
        // Given
        UserActive activeUser = User.create(new UserId(userId), email, hash, "John", null);
        when(tokenIssuer.isValidRefreshToken("valid.refresh")).thenReturn(true);
        when(tokenIssuer.extractUserIdFromRefreshToken("valid.refresh")).thenReturn(userId);
        when(userRepository.findById(new UserId(userId))).thenReturn(Optional.of(activeUser));
        when(tokenIssuer.issueAccessToken(userId, "user@example.com", "USER")).thenReturn("new.access.token");
        when(tokenIssuer.accessTokenExpirationSeconds()).thenReturn(900L);

        // When
        RefreshTokenUseCase.Result result = useCase.execute("valid.refresh");

        // Then
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.expiresInSeconds()).isEqualTo(900L);
        verify(tokenIssuer).issueAccessToken(eq(userId), eq("user@example.com"), eq("USER"));
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when refresh token is null")
    void shouldThrowException_whenTokenIsNull() {
        // When / Then
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid or expired refresh token");

        verify(tokenIssuer, never()).extractUserIdFromRefreshToken(any());
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when token is expired or invalid")
    void shouldThrowException_whenTokenIsInvalid() {
        // Given
        when(tokenIssuer.isValidRefreshToken("bad.token")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> useCase.execute("bad.token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when user no longer exists")
    void shouldThrowException_whenUserNotFound() {
        // Given
        when(tokenIssuer.isValidRefreshToken("valid.refresh")).thenReturn(true);
        when(tokenIssuer.extractUserIdFromRefreshToken("valid.refresh")).thenReturn(userId);
        when(userRepository.findById(any(UserId.class))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute("valid.refresh"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when account is inactive")
    void shouldThrowException_whenAccountIsInactive() {
        // Given
        UserInactive inactiveUser = User.createInvited(new UserId(userId), email, hash, "Invited");
        when(tokenIssuer.isValidRefreshToken("valid.refresh")).thenReturn(true);
        when(tokenIssuer.extractUserIdFromRefreshToken("valid.refresh")).thenReturn(userId);
        when(userRepository.findById(new UserId(userId))).thenReturn(Optional.of(inactiveUser));

        // When / Then
        assertThatThrownBy(() -> useCase.execute("valid.refresh"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Account is not active");

        verify(tokenIssuer, never()).issueAccessToken(any(), any(), any());
    }
}
