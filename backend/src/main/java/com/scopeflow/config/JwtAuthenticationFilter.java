package com.scopeflow.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * JWT authentication filter: validates Bearer token and populates SecurityContext.
 *
 * Sets UsernamePasswordAuthenticationToken with ScopeFlowPrincipal as principal,
 * enabling SecurityUtil to extract userId and workspaceId from any authenticated request.
 *
 * Hexagonal: injects UserRepository port (via UserStatusCacheService), not JPA directly.
 * Performance: user status is cached (TTL 5min) to avoid N+1 DB queries per request.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserStatusCacheService userStatusCacheService;

    public JwtAuthenticationFilter(JwtService jwtService, UserStatusCacheService userStatusCacheService) {
        this.jwtService = jwtService;
        this.userStatusCacheService = userStatusCacheService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtract(token);

            // Reject refresh tokens used as access tokens
            if ("refresh".equals(claims.get("type", String.class))) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String workspaceIdStr = claims.get("workspace_id", String.class);
            String role = claims.get("role", String.class);

            UUID workspaceId = workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : null;

            // Verify user still exists and is active.
            // UserStatusCacheService uses UserRepository (domain port) — not JPA directly.
            // Cache TTL: 5min (configured in application.properties via Caffeine spec).
            String status = userStatusCacheService.getUserStatus(userId);
            if (!"ACTIVE".equals(status)) {
                log.debug("Rejecting token for userId={}: status={}", userId, status);
                filterChain.doFilter(request, response);
                return;
            }

            ScopeFlowPrincipal principal = new ScopeFlowPrincipal(userId, email, workspaceId, role);
            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, authorities
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.debug("Invalid JWT in request to {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
