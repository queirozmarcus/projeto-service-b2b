package com.scopeflow.user.adapter.in.web.auth.dto;

public record AccessTokenResponse(
        String accessToken,
        long expiresIn
) {}
