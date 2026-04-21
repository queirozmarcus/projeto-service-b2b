package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.PasswordHasher;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticateUserUseCase")
class AuthenticateUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenIssuer tokenIssuer;

    private AuthenticateUserUseCase useCase;

    private final UUID userId = UUID.randomUUID();
    private final Email email = new Email("user@example.com");
    private final PasswordHash passwordHash = new PasswordHash("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, tokenIssuer);
    }

    @Test
    @DisplayName("should return tokens when credentials are valid")
    void shouldReturnTokens_whenCredentialsAreValid() {
        // Given
        UserActive activeUser = User.create(new UserId(userId), email, passwordHash, "John Doe", null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("rawpass", "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")).thenReturn(true);
        when(tokenIssuer.issueAccessToken(userId, "user@example.com", null, "USER")).thenReturn("access.token");
        when(tokenIssuer.issueRefreshToken(userId)).thenReturn("refresh.token");

        // When
        AuthenticateUserUseCase.Result result = useCase.execute("user@example.com", "rawpass");

        // Then
        assertThat(result.tokens().accessToken()).isEqualTo("access.token");
        assertThat(result.tokens().refreshToken()).isEqualTo("refresh.token");
        assertThat(result.user()).isEqualTo(activeUser);
        verify(tokenIssuer).issueAccessToken(userId, "user@example.com", null, "USER");
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when user does not exist")
    void shouldThrowException_whenUserNotFound() {
        // Given
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute("ghost@example.com", "pass"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when password does not match")
    void shouldThrowException_whenPasswordDoesNotMatch() {
        // Given
        UserActive activeUser = User.create(new UserId(userId), email, passwordHash, "Jane", null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches("wrongpass", "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> useCase.execute("user@example.com", "wrongpass"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("should throw InvalidCredentialsException when account is inactive")
    void shouldThrowException_whenAccountIsInactive() {
        // Given
        UserInactive inactiveUser = User.createInvited(new UserId(userId), email, passwordHash, "Invited");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(inactiveUser));

        // When / Then
        assertThatThrownBy(() -> useCase.execute("user@example.com", "pass"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Account is not active");

        verify(passwordHasher, never()).matches(any(), any());
        verify(tokenIssuer, never()).issueAccessToken(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should issue token with role USER (not null, not OWNER)")
    void shouldIssueTokenWithRoleUser() {
        // Given
        UserActive activeUser = User.create(new UserId(userId), email, passwordHash, "John", null);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(activeUser));
        when(passwordHasher.matches(any(), any())).thenReturn(true);
        when(tokenIssuer.issueAccessToken(any(), any(), any(), any())).thenReturn("token");
        when(tokenIssuer.issueRefreshToken(any())).thenReturn("refresh");

        // When
        useCase.execute("user@example.com", "pass");

        // Then — must pass "USER", not null, not "OWNER"
        verify(tokenIssuer).issueAccessToken(eq(userId), eq("user@example.com"), isNull(), eq("USER"));
    }
}
