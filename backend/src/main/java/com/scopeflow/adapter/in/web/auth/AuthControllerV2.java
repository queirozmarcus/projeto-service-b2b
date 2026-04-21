package com.scopeflow.adapter.in.web.auth;

import com.scopeflow.adapter.in.web.auth.dto.*;
import com.scopeflow.adapter.out.userservice.AuthProxyAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Auth controller: registration, login, token refresh, profile.
 *
 * Path: /api/v1/auth
 * Public endpoints: register, login, refresh
 * Protected: /me, /logout
 *
 * Security model:
 * - Access token: short-lived (15min), returned in response body, stored in memory by client
 * - Refresh token: long-lived (7d), delivered via httpOnly Set-Cookie, never exposed in body
 *
 * All auth requests are proxied to user-service via AuthProxyAdapter (circuit breaker + retry).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerV2.class);

    private final AuthProxyAdapter authProxyAdapter;

    public AuthControllerV2(AuthProxyAdapter authProxyAdapter) {
        this.authProxyAdapter = authProxyAdapter;
    }

    /**
     * POST /auth/register
     * Register a new user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/register")
    @RateLimit
    @Operation(summary = "Register new user account")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.debug("Proxying register to user-service: email={}", request.email());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return authProxyAdapter.proxy("/auth/register", HttpMethod.POST, request, headers);
    }

    /**
     * POST /auth/login
     * Authenticate user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/login")
    @RateLimit
    @Operation(summary = "Authenticate and obtain tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.debug("Proxying login to user-service: email={}", request.email());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return authProxyAdapter.proxy("/auth/login", HttpMethod.POST, request, headers);
    }

    /**
     * POST /auth/refresh
     * Exchange refresh token (from httpOnly cookie) for a new access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        log.info("Proxying refresh to user-service");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            headers.set("Cookie", cookieHeader);
        }
        return authProxyAdapter.proxy("/auth/refresh", HttpMethod.POST, null, headers);
    }

    /**
     * GET /auth/me
     * Return authenticated user's profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<?> me(HttpServletRequest request) {
        log.info("Proxying /me to user-service");
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        return authProxyAdapter.proxy("/auth/me", HttpMethod.GET, null, headers);
    }

    /**
     * POST /auth/logout
     * Clears the refresh token cookie. Client discards the access token from memory.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout: clears refresh token cookie")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        log.info("Proxying logout to user-service");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            headers.set("Cookie", cookieHeader);
        }
        return authProxyAdapter.proxy("/auth/logout", HttpMethod.POST, null, headers);
    }
}
