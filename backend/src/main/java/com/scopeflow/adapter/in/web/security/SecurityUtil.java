package com.scopeflow.adapter.in.web.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for extracting authenticated user context from JWT.
 *
 * JWT claims structure (expected):
 * {
 *   "sub": "user-uuid",
 *   "workspace_id": "workspace-uuid",
 *   "email": "user@example.com",
 *   "roles": ["OWNER", "MEMBER"],
 *   "exp": 1234567890
 * }
 */
public class SecurityUtil {

    /**
     * Extract workspace_id from JWT claims.
     *
     * @return workspace UUID from authenticated JWT
     * @throws SecurityException if not authenticated or claim missing
     */
    public static UUID getWorkspaceId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        // TODO: Replace with actual JWT claims extraction when Spring Security is configured
        // For now, this is a placeholder that assumes claims are in principal
        Object principal = auth.getPrincipal();

        // Placeholder: return a mock workspace_id for development
        // In production, this should extract from JWT claims:
        // JwtAuthenticationToken jwt = (JwtAuthenticationToken) auth;
        // UUID workspaceId = UUID.fromString(jwt.getToken().getClaim("workspace_id"));

        // For now, use a hardcoded test workspace (will be replaced with real JWT parsing)
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    /**
     * Extract user_id from JWT claims.
     *
     * @return user UUID from authenticated JWT
     * @throws SecurityException if not authenticated
     */
    public static UUID getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        // TODO: Extract from JWT subject claim
        // For now, placeholder
        return UUID.fromString("00000000-0000-0000-0000-000000000002");
    }

    /**
     * Check if current user has specific role.
     *
     * @param role role name (e.g., "OWNER", "ADMIN", "MEMBER")
     * @return true if user has role
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        return auth.getAuthorities().stream()
                .anyMatch(ga -> ga.getAuthority().equals("ROLE_" + role) || ga.getAuthority().equals(role));
    }
}
