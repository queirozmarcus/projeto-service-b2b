package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InviteUserUseCase")
class InviteUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;

    private InviteUserUseCase useCase;

    private final UserId invitedBy = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        useCase = new InviteUserUseCase(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("should create UserInactive with correct email when invite succeeds")
    void shouldCreateInactiveUser_whenInviteSucceeds() {
        // Given
        Email email = new Email("john.doe@example.com");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$temp_hash");

        // When
        UserInactive result = useCase.execute(email, invitedBy);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.canLogin()).isFalse();
        assertThat(result.status()).isEqualTo("INACTIVE");
        verify(userRepository).save(result);
    }

    @Test
    @DisplayName("should derive display name from email local part with dots")
    void shouldDeriveDisplayName_withDots() {
        // Given
        Email email = new Email("john.doe@example.com");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$hash");

        // When
        UserInactive result = useCase.execute(email, invitedBy);

        // Then — "john.doe" → "John Doe"
        assertThat(result.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("should derive display name from email local part with underscores")
    void shouldDeriveDisplayName_withUnderscores() {
        // Given
        Email email = new Email("maria_silva@example.com");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$hash");

        // When
        UserInactive result = useCase.execute(email, invitedBy);

        // Then — "maria_silva" → "Maria Silva"
        assertThat(result.getFullName()).isEqualTo("Maria Silva");
    }

    @Test
    @DisplayName("should derive display name from simple local part")
    void shouldDeriveDisplayName_fromSimpleLocalPart() {
        // Given
        Email email = new Email("alice@company.org");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$hash");

        // When
        UserInactive result = useCase.execute(email, invitedBy);

        // Then — "alice" → "Alice"
        assertThat(result.getFullName()).isEqualTo("Alice");
    }

    @Test
    @DisplayName("should generate a temporary password (not use invitedBy's password)")
    void shouldGenerateTempPassword_notUseInviterPassword() {
        // Given
        Email email = new Email("new@example.com");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$temp");

        // When
        useCase.execute(email, invitedBy);

        // Then — password hashing is called exactly once with some generated value
        verify(passwordHasher, times(1)).hash(anyString());
    }

    @Test
    @DisplayName("should propagate EmailAlreadyRegisteredException from repository (race condition guard)")
    void shouldPropagateException_whenEmailAlreadyExists() {
        // Given — repository throws (simulates UNIQUE constraint catch in JpaUserRepositoryAdapter)
        Email email = new Email("existing@example.com");
        when(passwordHasher.hash(anyString())).thenReturn("$2a$12$hash");
        doThrow(new EmailAlreadyRegisteredException("Email already registered: existing@example.com"))
                .when(userRepository).save(any());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(email, invitedBy))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("existing@example.com");
    }

    @Test
    @DisplayName("should throw NullPointerException when email is null")
    void shouldThrowException_whenEmailIsNull() {
        assertThatThrownBy(() -> useCase.execute(null, invitedBy))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email cannot be null");
    }

    @Test
    @DisplayName("should throw NullPointerException when invitedBy is null")
    void shouldThrowException_whenInvitedByIsNull() {
        assertThatThrownBy(() -> useCase.execute(new Email("a@b.com"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("InvitedBy cannot be null");
    }
}
