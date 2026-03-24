package com.scopeflow.config;

import java.util.UUID;

/**
 * Authenticated principal extracted from JWT claims.
 *
 * Stored in SecurityContext as the authentication principal,
 * enabling SecurityUtil.getUserId() and SecurityUtil.getWorkspaceId() to work correctly.
 */
public record ScopeFlowPrincipal(
        UUID userId,
        String email,
        UUID workspaceId,
        String role
) {}
