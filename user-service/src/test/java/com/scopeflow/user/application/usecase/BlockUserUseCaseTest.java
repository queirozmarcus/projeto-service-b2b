package com.scopeflow.user.application.usecase;

import com.scopeflow.user.domain.port.out.UserBlocklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockUserUseCase")
class BlockUserUseCaseTest {

    @Mock private UserBlocklist userBlocklist;

    private BlockUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BlockUserUseCase(userBlocklist);
    }

    @Test
    @DisplayName("should delegate block to UserBlocklist with correct userId and TTL")
    void shouldBlockUser_withCorrectTtl() {
        // Given
        String userId = UUID.randomUUID().toString();
        Duration ttl = Duration.ofMinutes(15);

        // When
        useCase.execute(userId, ttl);

        // Then
        verify(userBlocklist).block(userId, ttl);
    }

    @Test
    @DisplayName("should throw NullPointerException when userId is null")
    void shouldThrowException_whenUserIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null, Duration.ofMinutes(15)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userId cannot be null");
    }

    @Test
    @DisplayName("should throw NullPointerException when ttl is null")
    void shouldThrowException_whenTtlIsNull() {
        assertThatThrownBy(() -> useCase.execute("some-id", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("ttl cannot be null");
    }

    @Test
    @DisplayName("should not swallow exception when blocklist throws (fail-closed for block operation)")
    void shouldPropagateException_whenBlocklistFails() {
        // Given
        String userId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("Redis connection refused"))
                .when(userBlocklist).block(any(), any());

        // When / Then
        assertThatThrownBy(() -> useCase.execute(userId, Duration.ofMinutes(15)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Redis connection refused");
    }
}
