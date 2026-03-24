package com.scopeflow.adapter.in.web.auth.dto;

import com.scopeflow.core.domain.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile response DTO.
 */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPhone(),
                user.status(),
                user.getCreatedAt()
        );
    }
}
