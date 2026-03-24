package com.scopeflow.adapter.in.web.auth.dto;

import java.util.UUID;

/**
 * Token response: access token + refresh token pair.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UUID userId,
        String email,
        String fullName
) {}
