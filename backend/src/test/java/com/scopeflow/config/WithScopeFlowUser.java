package com.scopeflow.config;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation to populate SecurityContext with a ScopeFlowPrincipal.
 *
 * Usage:
 * {@code @WithScopeFlowUser}
 * or with specific workspace:
 * {@code @WithScopeFlowUser(workspaceId = "specific-uuid")}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithScopeFlowUserSecurityContextFactory.class)
public @interface WithScopeFlowUser {

    /** User UUID (defaults to a fixed test UUID for consistency) */
    String userId() default "00000000-0000-0000-0000-000000000001";

    /** Workspace UUID (defaults to a fixed test UUID for consistency) */
    String workspaceId() default "00000000-0000-0000-0000-000000000002";

    /** User email */
    String email() default "test@example.com";

    /** User role */
    String role() default "OWNER";
}
