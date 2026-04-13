package com.scopeflow.adapter.in.web.security;

import com.scopeflow.config.ScopeFlowPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for extracting authenticated user context from JWT claims.
 *
 * Reads from ScopeFlowPrincipal populated by JwtAuthenticationFilter.
 */
public class SecurityUtil {

    private SecurityUtil() {
        // Utility class
    }

    /**
     * Get current authenticated principal.
     *
     * @throws SecurityException if not authenticated
     */
    public static ScopeFlowPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof ScopeFlowPrincipal)) {
            throw new SecurityException("User not authenticated");
        }
        return (ScopeFlowPrincipal) auth.getPrincipal();
    }

    /**
     * Extract userId from authenticated JWT.
     */
    public static UUID getUserId() {
        return currentPrincipal().userId();
    }

    /**
     * Extract workspaceId from authenticated JWT.
     */
    public static UUID getWorkspaceId() {
        UUID workspaceId = currentPrincipal().workspaceId();
        if (workspaceId == null) {
            throw new SecurityException("No workspace_id in token claims");
        }
        return workspaceId;
    }

    /**
     * Check if current user has specific role.
     */
    public static boolean hasRole(String role) {
        try {
            return role.equals(currentPrincipal().role());
        } catch (SecurityException e) {
            return false;
        }
    }
}
