package com.scopeflow.adapter.in.web.auth.dto;

/**
 * Response for token refresh: new access token only.
 */
public record AccessTokenResponse(
        String accessToken,
        long expiresIn
) {}
