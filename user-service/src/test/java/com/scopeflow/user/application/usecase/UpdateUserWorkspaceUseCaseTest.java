package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.exception.UserStateException;
import com.scopeflow.user.domain.exception.WorkspaceAlreadyAssignedException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateUserWorkspaceUseCase")
class UpdateUserWorkspaceUseCaseTest {

    @Mock
    private UserRepository userRepository;

    private UpdateUserWorkspaceUseCase useCase;

    private static final String BCRYPT_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void setUp() {
        useCase = new UpdateUserWorkspaceUseCase(userRepository);
    }

    // ============ Helper builders ============

    private UserActive activeUserWithoutWorkspace(UserId id) {
        return new UserActive(id, new Email("user@example.com"), new PasswordHash(BCRYPT_HASH),
                "Test User", null, null, Instant.now(), Instant.now());
    }

    private UserActive activeUserWithWorkspace(UserId id, UUID workspaceId) {
        return new UserActive(id, new Email("user@example.com"), new PasswordHash(BCRYPT_HASH),
                "Test User", null, workspaceId, Instant.now(), Instant.now());
    }

    private UserInactive inactiveUserWithoutWorkspace(UserId id) {
        return new UserInactive(id, new Email("inactive@example.com"), new PasswordHash(BCRYPT_HASH),
                "Inactive User", null, null, Instant.now(), Instant.now());
    }

    private UserBlocked blockedUser(UserId id) {
        return new UserBlocked(id, new Email("blocked@example.com"), new PasswordHash(BCRYPT_HASH),
                "Blocked User", null, null, Instant.now(), Instant.now());
    }

    private UserDeleted deletedUser(UserId id) {
        return new UserDeleted(id, new Email("deleted@example.com"), new PasswordHash(BCRYPT_HASH),
                "Deleted User", null, null, Instant.now(), Instant.now());
    }

    // ============ Tests ============

    @Test
    @DisplayName("should update workspace when user is active and has no workspace")
    void shouldUpdateWorkspace_whenUserActiveAndNoWorkspace() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        UserActive user = activeUserWithoutWorkspace(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = useCase.execute(userId, workspaceId);

        // Then
        assertThat(result).isInstanceOf(UserActive.class);
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        verify(userRepository).save(argThat(u -> workspaceId.equals(u.getWorkspaceId())));
    }

    @Test
    @DisplayName("should be idempotent when same workspaceId is provided")
    void shouldBeIdempotent_whenSameWorkspaceIdProvided() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        UserActive user = activeUserWithWorkspace(userId, workspaceId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User result = useCase.execute(userId, workspaceId);

        // Then
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw UserNotFoundException when user does not exist")
    void shouldThrow_whenUserNotFound() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, workspaceId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw WorkspaceAlreadyAssignedException when user has a different workspace assigned")
    void shouldThrow_whenWorkspaceAlreadyAssigned() {
        // Given
        UserId userId = UserId.generate();
        UUID existingWorkspaceId = UUID.randomUUID();
        UUID newWorkspaceId = UUID.randomUUID();
        UserActive user = activeUserWithWorkspace(userId, existingWorkspaceId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, newWorkspaceId))
                .isInstanceOf(WorkspaceAlreadyAssignedException.class)
                .hasMessageContaining(userId.value().toString());

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw UserStateException when user is blocked")
    void shouldThrow_whenUserIsBlocked() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        UserBlocked user = blockedUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, workspaceId))
                .isInstanceOf(UserStateException.class)
                .hasMessageContaining("BLOCKED");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw UserStateException when user is deleted")
    void shouldThrow_whenUserIsDeleted() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        UserDeleted user = deletedUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, workspaceId))
                .isInstanceOf(UserStateException.class)
                .hasMessageContaining("DELETED");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw NullPointerException when workspaceId is null")
    void shouldThrow_whenWorkspaceIdIsNull() {
        // Given
        UserId userId = UserId.generate();

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("workspaceId");

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("should update workspace when user is inactive and has no workspace")
    void shouldUpdateWorkspace_whenUserInactiveAndNoWorkspace() {
        // Given
        UserId userId = UserId.generate();
        UUID workspaceId = UUID.randomUUID();
        UserInactive user = inactiveUserWithoutWorkspace(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        User result = useCase.execute(userId, workspaceId);

        // Then
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        verify(userRepository).save(any());
    }
}
