package com.scopeflow.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.List;
import java.util.UUID;

/**
 * Factory that creates a SecurityContext with a ScopeFlowPrincipal.
 *
 * Used by @WithScopeFlowUser annotation in @WebMvcTest slices.
 */
public class WithScopeFlowUserSecurityContextFactory
        implements WithSecurityContextFactory<WithScopeFlowUser> {

    @Override
    public SecurityContext createSecurityContext(WithScopeFlowUser annotation) {
        UUID userId = UUID.fromString(annotation.userId());
        UUID workspaceId = UUID.fromString(annotation.workspaceId());
        String email = annotation.email();
        String role = annotation.role();

        ScopeFlowPrincipal principal = new ScopeFlowPrincipal(userId, email, workspaceId, role);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
