package com.scopeflow.user.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for extracting authenticated user context from JWT claims.
 */
public class SecurityUtil {

    private SecurityUtil() {}

    public static ScopeFlowPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof ScopeFlowPrincipal)) {
            throw new SecurityException("User not authenticated");
        }
        return (ScopeFlowPrincipal) auth.getPrincipal();
    }

    public static UUID getUserId() {
        return currentPrincipal().userId();
    }
}
