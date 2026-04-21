package com.scopeflow.adapter.in.web.auth.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * User profile response DTO.
 * Post-migration: populated from user-service responses, not domain User.
 */
public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String phone,
        String status,
        Instant createdAt
) {
}
