package com.scopeflow.user.config;

import com.scopeflow.user.domain.port.out.UserBlocklist;
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
 * JWT authentication filter for user-service.
 *
 * Validates Bearer token signature and expiration via JwtService, then checks
 * the Redis blocklist (O(1)) to detect accounts deactivated after token issuance.
 * No DB hit per request — only blocked users incur a Redis lookup.
 *
 * Fail-open: if Redis is unavailable, UserBlocklist.isBlocked() returns false
 * and the request proceeds normally (JWT validity is still enforced).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserBlocklist userBlocklist;

    public JwtAuthenticationFilter(JwtService jwtService, UserBlocklist userBlocklist) {
        this.jwtService = jwtService;
        this.userBlocklist = userBlocklist;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.validateAndExtract(token);

            if (claims == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Reject refresh tokens used as access tokens
            if ("refresh".equals(claims.get("type", String.class))) {
                filterChain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(claims.getSubject());

            // O(1) Redis lookup — replaces per-request DB query.
            // isBlocked() is fail-open: returns false if Redis is unavailable.
            if (userBlocklist.isBlocked(userId.toString())) {
                log.debug("Rejecting token for blocked userId={}", userId);
                filterChain.doFilter(request, response);
                return;
            }

            String email = claims.get("email", String.class);
            String workspaceIdStr = claims.get("workspace_id", String.class);
            String role = claims.get("role", String.class);
            UUID workspaceId = workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : null;

            ScopeFlowPrincipal principal = new ScopeFlowPrincipal(userId, email, workspaceId, role);
            List<SimpleGrantedAuthority> authorities = role != null
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    : List.of();

            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException e) {
            log.debug("Invalid JWT in request to {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
