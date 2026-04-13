package com.scopeflow.user.config;

import java.util.UUID;

/**
 * Authenticated principal extracted from JWT claims.
 */
public record ScopeFlowPrincipal(
        UUID userId,
        String email,
        UUID workspaceId,
        String role
) {}
