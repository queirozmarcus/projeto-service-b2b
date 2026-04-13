package com.scopeflow.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * Tests C1: Cache-backed user status check prevents DB queries on every request.
 * Tests token validation scenarios: expired, invalid signature, refresh token misuse.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserStatusCacheService userStatusCacheService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String BEARER_TOKEN = "Bearer " + VALID_TOKEN;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("C1: JWT cache behavior")
    class CacheBehaviorTests {

        @Test
        @DisplayName("should delegate to UserStatusCacheService (cache hit or miss)")
        void shouldDelegateToUserStatusCacheService() throws Exception {
            // Given — valid JWT for active user
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(WORKSPACE_ID.toString());
            given(claims.get("role", String.class)).willReturn("OWNER");
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn("ACTIVE");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — cache service was called exactly once (not repository directly)
            verify(userStatusCacheService).getUserStatus(USER_ID);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should populate SecurityContext when user is ACTIVE")
        void shouldPopulateSecurityContext_whenUserIsActive() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(WORKSPACE_ID.toString());
            given(claims.get("role", String.class)).willReturn("OWNER");
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn("ACTIVE");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — SecurityContext populated
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(ScopeFlowPrincipal.class);

            ScopeFlowPrincipal principal = (ScopeFlowPrincipal) authentication.getPrincipal();
            assertThat(principal.userId()).isEqualTo(USER_ID);
            assertThat(principal.workspaceId()).isEqualTo(WORKSPACE_ID);
        }

        @Test
        @DisplayName("should NOT populate SecurityContext when user is INACTIVE")
        void shouldSkipAuthentication_whenUserIsInactive() throws Exception {
            // Given — user exists but is INACTIVE (e.g., invited but not activated)
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(WORKSPACE_ID.toString());
            given(claims.get("role", String.class)).willReturn("OWNER");
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn("INACTIVE");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — SecurityContext NOT populated
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should NOT populate SecurityContext when user status is null (not found)")
        void shouldSkipAuthentication_whenUserNotFound() throws Exception {
            // Given — user deleted or missing from DB/cache
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(WORKSPACE_ID.toString());
            given(claims.get("role", String.class)).willReturn("OWNER");
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — no auth
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    @Nested
    @DisplayName("Token validation")
    class TokenValidationTests {

        @Test
        @DisplayName("should skip filter when no Authorization header")
        void shouldSkipFilter_whenNoAuthHeader() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — filter chain continues, no auth attempted
            verifyNoInteractions(jwtService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should skip filter when Authorization header is not Bearer")
        void shouldSkipFilter_whenNotBearerToken() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn("Basic dXNlcjpwYXNz");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(jwtService);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("should NOT authenticate when token signature is invalid")
        void shouldNotAuthenticate_whenInvalidSignature() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN))
                    .willThrow(new JwtException("JWT signature does not match"));

            // When — no exception should propagate
            filter.doFilterInternal(request, response, filterChain);

            // Then — SecurityContext empty, chain continues
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(userStatusCacheService);
        }

        @Test
        @DisplayName("should NOT authenticate when token is expired")
        void shouldNotAuthenticate_whenTokenExpired() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN))
                    .willThrow(new JwtException("JWT expired"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should NOT authenticate when refresh token is used as access token")
        void shouldNotAuthenticate_whenRefreshTokenUsedAsAccessToken() throws Exception {
            // Given — token has type=refresh (should only be used at /auth/refresh)
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("refresh");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — filter chain continues but no auth set
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(userStatusCacheService);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should set ROLE_ authority on authentication token")
        void shouldSetRoleAuthority_onAuthentication() throws Exception {
            // Given
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(WORKSPACE_ID.toString());
            given(claims.get("role", String.class)).willReturn("ADMIN");
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn("ACTIVE");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — authority is ROLE_ADMIN
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth.getAuthorities())
                    .extracting(a -> a.getAuthority())
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("should handle token without workspace_id claim (no workspace context)")
        void shouldHandleToken_withoutWorkspaceId() throws Exception {
            // Given — user not yet associated with a workspace
            given(request.getHeader("Authorization")).willReturn(BEARER_TOKEN);
            given(jwtService.validateAndExtract(VALID_TOKEN)).willReturn(claims);
            given(claims.get("type", String.class)).willReturn("access");
            given(claims.getSubject()).willReturn(USER_ID.toString());
            given(claims.get("email", String.class)).willReturn("user@example.com");
            given(claims.get("workspace_id", String.class)).willReturn(null);
            given(claims.get("role", String.class)).willReturn(null);
            given(userStatusCacheService.getUserStatus(USER_ID)).willReturn("ACTIVE");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then — auth set with null workspaceId and empty authorities
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            ScopeFlowPrincipal principal = (ScopeFlowPrincipal) auth.getPrincipal();
            assertThat(principal.workspaceId()).isNull();
            assertThat(auth.getAuthorities()).isEmpty();
        }
    }
}
