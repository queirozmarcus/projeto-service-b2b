package com.scopeflow.user.adapter.in.web.user.dto;

import com.scopeflow.user.domain.model.User;

import java.time.Instant;
import java.util.UUID;

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
                user.getEmail().normalized(),
                user.getFullName(),
                user.getPhone(),
                user.status(),
                user.getCreatedAt()
        );
    }
}
