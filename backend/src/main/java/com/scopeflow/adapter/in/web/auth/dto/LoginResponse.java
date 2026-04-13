package com.scopeflow.adapter.in.web.auth.dto;

import java.util.UUID;

/**
 * Login response: access token only. Refresh token is delivered via Set-Cookie (httpOnly).
 */
public record LoginResponse(
        String accessToken,
        long expiresIn,
        UUID userId,
        String email,
        String fullName
) {}
