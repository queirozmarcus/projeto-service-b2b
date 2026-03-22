package com.scopeflow.core.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for User domain model.
 * Pure Java tests (no Spring, no DB).
 */
@DisplayName("User Domain Model Tests")
class UserTest {

    @Nested
    @DisplayName("User Creation")
    class UserCreation {

        @Test
        void shouldCreateUserActive() {
            // Given
            UserId userId = UserId.generate();
            Email email = new Email("user@example.com");
            PasswordHash passwordHash = new PasswordHash("$2b$12$n3u2Hu6WkdB/wSXx3lxUo.CZLrVr8T5SvELe7x6P8yW9sV4B8K.vK");

            // When
            UserActive user = User.create(userId, email, passwordHash, "John Doe", "+5511999999999");

            // Then
            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(userId);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFullName()).isEqualTo("John Doe");
            assertThat(user.status()).isEqualTo("ACTIVE");
            assertThat(user.canLogin()).isTrue();
        }

        @Test
        void shouldThrowOnNullEmail() {
            // When & Then
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldThrowOnInvalidEmailFormat() {
            // When & Then
            assertThatThrownBy(() -> new Email("invalid-email"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        void shouldThrowOnEmptyEmail() {
            // When & Then
            assertThatThrownBy(() -> new Email(""))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Email Value Object")
    class EmailValueObject {

        @Test
        void shouldNormalizeEmail() {
            // Given
            Email email = new Email("USER@EXAMPLE.COM");

            // When
            String normalized = email.normalized();

            // Then
            assertThat(normalized).isEqualTo("user@example.com");
        }

        @Test
        void shouldAcceptValidEmails() {
            // Given valid emails
            String[] validEmails = {
                    "user@example.com",
                    "john.doe+tag@subdomain.co.uk",
                    "test_email-2024@example.org"
            };

            // When & Then (no exception)
            for (String validEmail : validEmails) {
                assertThatCode(() -> new Email(validEmail))
                        .doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("PasswordHash Value Object")
    class PasswordHashValueObject {

        @Test
        void shouldAcceptValidBcryptHash() {
            // Given valid bcrypt hash
            String validHash = "$2b$12$n3u2Hu6WkdB/wSXx3lxUo.CZLrVr8T5SvELe7x6P8yW9sV4B8K.vK";

            // When & Then
            assertThatCode(() -> new PasswordHash(validHash))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldThrowOnInvalidBcryptFormat() {
            // When & Then
            assertThatThrownBy(() -> new PasswordHash("plaintext-password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid bcrypt hash format");
        }
    }

    @Nested
    @DisplayName("User States (Sealed Class)")
    class UserStates {

        @Test
        void shouldDifferentiateUserStates() {
            // Given
            UserId userId = UserId.generate();
            Email email = new Email("user@example.com");
            PasswordHash passwordHash = new PasswordHash("$2b$12$n3u2Hu6WkdB/wSXx3lxUo.CZLrVr8T5SvELe7x6P8yW9sV4B8K.vK");

            // When
            UserActive active = User.create(userId, email, passwordHash, "John", null);
            UserInactive inactive = new UserInactive(userId, email, passwordHash, "John", null, active.getCreatedAt(), active.getUpdatedAt());
            UserDeleted deleted = new UserDeleted(userId, email, passwordHash, "John", null, active.getCreatedAt(), active.getUpdatedAt());

            // Then
            assertThat(active.status()).isEqualTo("ACTIVE");
            assertThat(active.canLogin()).isTrue();

            assertThat(inactive.status()).isEqualTo("INACTIVE");
            assertThat(inactive.canLogin()).isFalse();

            assertThat(deleted.status()).isEqualTo("DELETED");
            assertThat(deleted.canLogin()).isFalse();
        }
    }

    @Nested
    @DisplayName("UserId Value Object")
    class UserIdValueObject {

        @Test
        void shouldGenerateUniqueIds() {
            // When
            UserId id1 = UserId.generate();
            UserId id2 = UserId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void shouldParseFromString() {
            // Given
            String uuidString = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

            // When
            UserId userId = UserId.of(uuidString);

            // Then
            assertThat(userId.value().toString()).isEqualTo(uuidString);
        }
    }
}
