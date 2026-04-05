package com.scopeflow.core.domain.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService service;

    // A real bcrypt hash — required by PasswordHash value object validation
    private static final String BCRYPT_HASH =
            "$2b$12$n3u2Hu6WkdB/wSXx3lxUo.CZLrVr8T5SvELe7x6P8yW9sV4B8K.vK";

    private static final Email EMAIL = new Email("alice@example.com");
    private static final PasswordHash PASSWORD_HASH = new PasswordHash(BCRYPT_HASH);

    // ============ registerUser ============

    @Nested
    @DisplayName("registerUser")
    class RegisterUser {

        @Test
        @DisplayName("should create UserActive, persist it and return it when email is unique")
        void shouldCreateAndSaveUser_whenEmailIsUnique() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when
            UserActive result = service.registerUser(EMAIL, PASSWORD_HASH, "Alice Oliveira", "+5511999990000");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.canLogin()).isTrue();
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getFullName()).isEqualTo("Alice Oliveira");
            assertThat(result.getPhone()).isEqualTo("+5511999990000");
            assertThat(result.getId()).isNotNull();
            assertThat(result.getCreatedAt()).isNotNull();

            verify(userRepository).save(any(UserActive.class));
        }

        @Test
        @DisplayName("should create UserActive with null phone (optional field)")
        void shouldCreateUser_withNullPhone() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when
            UserActive result = service.registerUser(EMAIL, PASSWORD_HASH, "Bob Silva", null);

            // then
            assertThat(result.getPhone()).isNull();
            verify(userRepository).save(any(UserActive.class));
        }

        @Test
        @DisplayName("should throw EmailAlreadyRegisteredException when email is already registered")
        void shouldThrow_whenEmailAlreadyExists() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when / then
            assertThatThrownBy(() ->
                    service.registerUser(EMAIL, PASSWORD_HASH, "Alice Oliveira", null))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .hasMessageContaining(EMAIL.normalized());

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should carry ERROR_CODE USER-001 in exception")
        void shouldCarryErrorCode_inException() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when / then
            assertThatThrownBy(() ->
                    service.registerUser(EMAIL, PASSWORD_HASH, "Alice", null))
                    .isInstanceOf(EmailAlreadyRegisteredException.class)
                    .satisfies(ex -> assertThat(((EmailAlreadyRegisteredException) ex).getErrorCode())
                            .isEqualTo("USER-001"));
        }

        @Test
        @DisplayName("should throw NullPointerException when UserRepository is null at construction")
        void shouldThrow_whenRepositoryIsNull() {
            assertThatThrownBy(() -> new UserService(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ============ deactivateUser ============

    @Nested
    @DisplayName("deactivateUser")
    class DeactivateUser {

        @Test
        @DisplayName("should delete user when user exists")
        void shouldDeleteUser_whenUserExists() {
            // given
            UserId userId = UserId.generate();
            UserActive user = User.create(userId, EMAIL, PASSWORD_HASH, "Alice", null);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            service.deactivateUser(userId);

            // then
            verify(userRepository).delete(userId);
        }

        @Test
        @DisplayName("should be silent (no exception) when user does not exist")
        void shouldBeSilent_whenUserNotFound() {
            // given
            UserId userId = UserId.generate();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when — must not throw
            service.deactivateUser(userId);

            // then — delete is never called because user was not found
            verify(userRepository, never()).delete(any());
        }
    }

    // ============ getUserById ============

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUser_whenFound() {
            // given
            UserId userId = UserId.generate();
            UserActive user = User.create(userId, EMAIL, PASSWORD_HASH, "Alice", null);
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            var result = service.getUserById(userId);

            // then
            assertThat(result).isPresent().get().isEqualTo(user);
        }

        @Test
        @DisplayName("should return empty when user not found")
        void shouldReturnEmpty_whenNotFound() {
            // given
            UserId userId = UserId.generate();
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when
            var result = service.getUserById(userId);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ============ saveInvitedUser ============

    @Nested
    @DisplayName("saveInvitedUser")
    class SaveInvitedUser {

        @Test
        @DisplayName("should persist UserInactive without email-uniqueness check")
        void shouldSaveInvitedUser_withoutUniquenessCheck() {
            // given
            UserInactive invited = (UserInactive) User.createInvited(
                    UserId.generate(), new Email("invited@example.com"),
                    PASSWORD_HASH, "Invited User"
            );

            // when
            service.saveInvitedUser(invited);

            // then
            verify(userRepository).save(invited);
            // existsByEmail is NEVER called — bypass is intentional
            verify(userRepository, never()).existsByEmail(any());
        }

        @Test
        @DisplayName("should throw NullPointerException when invited user is null")
        void shouldThrow_whenInvitedUserIsNull() {
            assertThatThrownBy(() -> service.saveInvitedUser(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
