package com.scopeflow.user.adapter.in.web.user.dto;

import com.scopeflow.user.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        Instant createdAt
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId().value(),
                user.getEmail().normalized(),
                user.getFullName(),
                user.getPhone(),
                user.status(),
                user.getCreatedAt()
        );
    }
}
