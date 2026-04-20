package com.scopeflow.user.adapter.in.web.auth.dto;

import com.scopeflow.user.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        Instant createdAt
) {
    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPhone(),
                user.status(),
                user.getCreatedAt()
        );
    }
}
