package com.scopeflow.adapter.in.web.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh token request DTO.
 */
public record RefreshTokenRequest(
        @NotBlank
        String refreshToken
) {}
