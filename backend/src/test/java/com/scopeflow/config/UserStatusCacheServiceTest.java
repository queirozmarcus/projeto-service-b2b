package com.scopeflow.config;

import com.scopeflow.core.domain.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for UserStatusCacheService.
 *
 * Tests C1: cache correctly resolves user status.
 * Integration-level cache behaviour (actual TTL/eviction) is tested in
 * JwtCacheIntegrationTest with a real Spring context.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserStatusCacheService")
class UserStatusCacheServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserStatusCacheService service;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UserStatusCacheService(userRepository);
    }

    @Test
    @DisplayName("should return ACTIVE status when user is active")
    void shouldReturnActive_whenUserIsActive() {
        // Given
        UserId userId = new UserId(USER_ID);
        UserActive activeUser = new UserActive(
                userId,
                new Email("user@example.com"),
                new PasswordHash("hash"),
                "Test User", null,
                Instant.now(), Instant.now()
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(activeUser));

        // When
        String status = service.getUserStatus(USER_ID);

        // Then
        assertThat(status).isEqualTo("ACTIVE");
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("should return null when user does not exist in repository")
    void shouldReturnNull_whenUserNotFound() {
        // Given — cache miss for unknown user
        UserId userId = new UserId(USER_ID);
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When
        String status = service.getUserStatus(USER_ID);

        // Then
        assertThat(status).isNull();
    }

    @Test
    @DisplayName("should return INACTIVE status when user is invited")
    void shouldReturnInactive_whenUserIsInactive() {
        // Given
        UserId userId = new UserId(USER_ID);
        UserInactive inactiveUser = new UserInactive(
                userId,
                new Email("invited@example.com"),
                new PasswordHash("temp-hash"),
                "Invited User",
                null,
                Instant.now(), Instant.now()
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

        // When
        String status = service.getUserStatus(USER_ID);

        // Then
        assertThat(status).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("should return DELETED status when user is deleted")
    void shouldReturnDeleted_whenUserIsDeleted() {
        // Given
        UserId userId = new UserId(USER_ID);
        UserDeleted deletedUser = new UserDeleted(
                userId,
                new Email("deleted@example.com"),
                new PasswordHash("hash"),
                "Deleted User", null,
                Instant.now(), Instant.now()
        );
        given(userRepository.findById(userId)).willReturn(Optional.of(deletedUser));

        // When
        String status = service.getUserStatus(USER_ID);

        // Then
        assertThat(status).isEqualTo("DELETED");
    }
}
