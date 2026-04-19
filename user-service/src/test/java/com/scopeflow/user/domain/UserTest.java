package com.scopeflow.user.domain;

import com.scopeflow.user.domain.common.InvalidValueObjectException;
import com.scopeflow.user.domain.model.*;
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

    private static final String VALID_BCRYPT_HASH = "$2b$12$n3u2Hu6WkdB/wSXx3lxUo.CZLrVr8T5SvELe7x6P8yW9sV4B8K.vK";

    @Nested
    @DisplayName("User Creation")
    class UserCreation {

        @Test
        void shouldCreateUserActive() {
            UserId userId = UserId.generate();
            Email email = new Email("user@example.com");
            PasswordHash passwordHash = new PasswordHash(VALID_BCRYPT_HASH);

            UserActive user = User.create(userId, email, passwordHash, "John Doe", "+5511999999999");

            assertThat(user).isNotNull();
            assertThat(user.getId()).isEqualTo(userId);
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(user.getFullName()).isEqualTo("John Doe");
            assertThat(user.status()).isEqualTo("ACTIVE");
            assertThat(user.canLogin()).isTrue();
        }

        @Test
        void shouldCreateInvitedUser() {
            UserId userId = UserId.generate();
            Email email = new Email("invited@example.com");
            PasswordHash hash = new PasswordHash(VALID_BCRYPT_HASH);

            UserInactive user = User.createInvited(userId, email, hash, "Invited User");

            assertThat(user.status()).isEqualTo("INACTIVE");
            assertThat(user.canLogin()).isFalse();
            assertThat(user.getPhone()).isNull();
        }

        @Test
        void shouldThrowOnNullEmail() {
            assertThatThrownBy(() -> new Email(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldThrowOnInvalidEmailFormat() {
            assertThatThrownBy(() -> new Email("invalid-email"))
                    .isInstanceOf(InvalidValueObjectException.class)
                    .hasMessageContaining("Invalid email format");
        }

        @Test
        void shouldThrowOnEmptyEmail() {
            assertThatThrownBy(() -> new Email(""))
                    .isInstanceOf(InvalidValueObjectException.class);
        }
    }

    @Nested
    @DisplayName("Email Value Object")
    class EmailValueObject {

        @Test
        void shouldNormalizeEmail() {
            Email email = new Email("USER@EXAMPLE.COM");
            assertThat(email.normalized()).isEqualTo("user@example.com");
        }

        @Test
        void shouldAcceptValidEmails() {
            String[] validEmails = {
                    "user@example.com",
                    "john.doe+tag@subdomain.co.uk",
                    "test_email-2024@example.org"
            };
            for (String validEmail : validEmails) {
                assertThatCode(() -> new Email(validEmail)).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("PasswordHash Value Object")
    class PasswordHashValueObject {

        @Test
        void shouldAcceptValidBcryptHash() {
            assertThatCode(() -> new PasswordHash(VALID_BCRYPT_HASH)).doesNotThrowAnyException();
        }

        @Test
        void shouldThrowOnInvalidBcryptFormat() {
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
            UserId userId = UserId.generate();
            Email email = new Email("user@example.com");
            PasswordHash hash = new PasswordHash(VALID_BCRYPT_HASH);

            UserActive active = User.create(userId, email, hash, "John", null);
            UserInactive inactive = new UserInactive(userId, email, hash, "John", null, active.getCreatedAt(), active.getUpdatedAt());
            UserDeleted deleted = new UserDeleted(userId, email, hash, "John", null, active.getCreatedAt(), active.getUpdatedAt());

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
            UserId id1 = UserId.generate();
            UserId id2 = UserId.generate();
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void shouldParseFromString() {
            String uuidString = "f47ac10b-58cc-4372-a567-0e02b2c3d479";
            UserId userId = UserId.of(uuidString);
            assertThat(userId.value().toString()).isEqualTo(uuidString);
        }
    }
}
