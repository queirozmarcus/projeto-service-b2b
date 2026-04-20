package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.model.Email;
import com.scopeflow.user.domain.model.UserActive;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegisterUserUseCase")
class RegisterUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordHasher passwordHasher;

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, passwordHasher);
    }

    @Test
    @DisplayName("should register user and return UserActive when email is new")
    void shouldRegisterUser_whenEmailIsNew() {
        // Given
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(false);
        when(passwordHasher.hash("secret123")).thenReturn("$2a$12$hashed");

        // When
        UserActive result = useCase.execute("User@Example.COM", "secret123", "John Doe", null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail().normalized()).isEqualTo("user@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
        assertThat(result.canLogin()).isTrue();
        verify(userRepository).save(result);
    }

    @Test
    @DisplayName("should normalize email to lowercase before checking repository")
    void shouldNormalizeEmail_beforeCheckingRepository() {
        // Given
        when(userRepository.existsByEmail(new Email("user@example.com"))).thenReturn(false);
        when(passwordHasher.hash(any())).thenReturn("$2a$12$hashed");

        // When
        UserActive result = useCase.execute("  USER@EXAMPLE.COM  ", "pass", "Name", null);

        // Then
        assertThat(result.getEmail().value()).isEqualTo("user@example.com");
        verify(userRepository).existsByEmail(new Email("user@example.com"));
    }

    @Test
    @DisplayName("should throw EmailAlreadyRegisteredException when email exists")
    void shouldThrowException_whenEmailAlreadyRegistered() {
        // Given
        when(userRepository.existsByEmail(any(Email.class))).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> useCase.execute("taken@example.com", "pass", "Name", null))
                .isInstanceOf(EmailAlreadyRegisteredException.class)
                .hasMessageContaining("taken@example.com");

        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when email format is invalid")
    void shouldThrowException_whenEmailIsInvalid() {
        // When / Then
        assertThatThrownBy(() -> useCase.execute("not-an-email", "pass", "Name", null))
                .hasMessageContaining("Invalid email format");

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    @DisplayName("should hash password and never store raw password")
    void shouldHashPassword_andNeverStoreRawPassword() {
        // Given
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(passwordHasher.hash("rawpassword")).thenReturn("$2a$12$hashed_value");

        // When
        UserActive result = useCase.execute("user@example.com", "rawpassword", "Name", null);

        // Then
        assertThat(result.getPasswordHash().value()).isEqualTo("$2a$12$hashed_value");
        assertThat(result.getPasswordHash().value()).doesNotContain("rawpassword");
        verify(passwordHasher).hash("rawpassword");
    }
}
