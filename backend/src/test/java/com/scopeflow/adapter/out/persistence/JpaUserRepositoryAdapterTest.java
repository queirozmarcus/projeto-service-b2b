package com.scopeflow.adapter.out.persistence;

import com.scopeflow.adapter.out.persistence.user.JpaUser;
import com.scopeflow.adapter.out.persistence.user.JpaUserRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.user.JpaUserSpringRepository;
import com.scopeflow.core.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUserRepositoryAdapter")
class JpaUserRepositoryAdapterTest {

    @Mock
    private JpaUserSpringRepository springRepo;

    @InjectMocks
    private JpaUserRepositoryAdapter adapter;

    private UUID userId;
    private JpaUser jpaUser;

    // BCrypt hash for "Password1!" (pre-computed for test)
    private static final String BCRYPT_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jpaUser = new JpaUser(
                userId, "user@example.com", BCRYPT_HASH,
                "Test User", "+5511999999999", "ACTIVE",
                Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("should return UserActive when found by ID with ACTIVE status")
    void findById_shouldReturnUserActive_whenActiveExists() {
        // Given
        given(springRepo.findById(userId)).willReturn(Optional.of(jpaUser));

        // When
        Optional<User> result = adapter.findById(new UserId(userId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(UserActive.class);
        assertThat(result.get().getEmail().normalized()).isEqualTo("user@example.com");
        assertThat(result.get().getFullName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("should return empty when user not found")
    void findById_shouldReturnEmpty_whenNotExists() {
        // Given
        given(springRepo.findById(any(UUID.class))).willReturn(Optional.empty());

        // When
        Optional<User> result = adapter.findById(UserId.generate());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return UserInactive when found with INACTIVE status")
    void findById_shouldReturnUserInactive_whenInactiveStatus() {
        // Given
        JpaUser inactiveUser = new JpaUser(
                userId, "inactive@example.com", BCRYPT_HASH,
                "Inactive User", null, "INACTIVE",
                Instant.now(), Instant.now()
        );
        given(springRepo.findById(userId)).willReturn(Optional.of(inactiveUser));

        // When
        Optional<User> result = adapter.findById(new UserId(userId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(UserInactive.class);
        assertThat(result.get().canLogin()).isFalse();
    }

    @Test
    @DisplayName("should return UserDeleted when found with DELETED status")
    void findById_shouldReturnUserDeleted_whenDeletedStatus() {
        // Given
        JpaUser deletedUser = new JpaUser(
                userId, "deleted@example.com", BCRYPT_HASH,
                "Deleted User", null, "DELETED",
                Instant.now(), Instant.now()
        );
        given(springRepo.findById(userId)).willReturn(Optional.of(deletedUser));

        // When
        Optional<User> result = adapter.findById(new UserId(userId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(UserDeleted.class);
        assertThat(result.get().canLogin()).isFalse();
    }

    @Test
    @DisplayName("should check email existence via spring repository")
    void existsByEmail_shouldDelegateToSpringRepo() {
        // Given
        Email email = new Email("user@example.com");
        given(springRepo.existsByEmail("user@example.com")).willReturn(true);

        // When
        boolean exists = adapter.existsByEmail(email);

        // Then
        assertThat(exists).isTrue();
        then(springRepo).should().existsByEmail("user@example.com");
    }

    @Test
    @DisplayName("should soft-delete user by setting status to DELETED")
    void delete_shouldSetStatusToDeleted() {
        // Given
        given(springRepo.findById(userId)).willReturn(Optional.of(jpaUser));
        given(springRepo.save(any())).willReturn(jpaUser);

        // When
        adapter.delete(new UserId(userId));

        // Then
        then(springRepo).should().save(any(JpaUser.class));
        assertThat(jpaUser.getStatus()).isEqualTo("DELETED");
    }

    @Test
    @DisplayName("should throw for unknown status in toDomain")
    void findById_shouldThrow_whenUnknownStatus() {
        // Given
        JpaUser unknownStatus = new JpaUser(
                userId, "x@example.com", BCRYPT_HASH,
                "Unknown", null, "UNKNOWN_STATUS",
                Instant.now(), Instant.now()
        );
        given(springRepo.findById(userId)).willReturn(Optional.of(unknownStatus));

        // When / Then
        assertThatThrownBy(() -> adapter.findById(new UserId(userId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN_STATUS");
    }
}
