package com.scopeflow.adapter.in.web.user.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * User response DTO.
 * Used for cross-service communication with user-service.
 * Post-migration: no longer maps from domain User (decommissioned).
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
