package com.scopeflow.adapter.in.web.auth;

import com.scopeflow.adapter.in.web.auth.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
 * All auth requests are proxied to user-service.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerV2.class);

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private final RestTemplate authServiceRestTemplate;

    public AuthControllerV2(RestTemplate authServiceRestTemplate) {
        this.authServiceRestTemplate = authServiceRestTemplate;
    }

    /**
     * POST /auth/register
     * Register a new user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/register")
    @RateLimit
    @Operation(summary = "Register new user account")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Proxying register to user-service: email={}", request.email());
        return proxyPost("/auth/register", request);
    }

    /**
     * POST /auth/login
     * Authenticate user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/login")
    @RateLimit
    @Operation(summary = "Authenticate and obtain tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        log.info("Proxying login to user-service: email={}", request.email());
        return proxyPost("/auth/login", request);
    }

    /**
     * POST /auth/refresh
     * Exchange refresh token (from httpOnly cookie) for a new access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        log.info("Proxying refresh to user-service");
        return proxyPostWithCookies("/auth/refresh", null, request);
    }

    /**
     * GET /auth/me
     * Return authenticated user's profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<?> me(HttpServletRequest request) {
        log.info("Proxying /me to user-service");
        return proxyGetWithAuth("/auth/me", request);
    }

    /**
     * POST /auth/logout
     * Clears the refresh token cookie. Client discards the access token from memory.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout: clears refresh token cookie")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        log.info("Proxying logout to user-service");
        return proxyPostWithCookies("/auth/logout", null, request);
    }

    // ============ Proxy helpers ============

    /**
     * Proxy a POST request to user-service.
     * Forwards request body and returns response with headers (including Set-Cookie).
     */
    private ResponseEntity<?> proxyPost(String path, Object body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.POST, entity, String.class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            // Forward Set-Cookie header from user-service (contains refresh token)
            if (response.getHeaders().containsKey(HttpHeaders.SET_COOKIE)) {
                responseHeaders.addAll(HttpHeaders.SET_COOKIE, response.getHeaders().get(HttpHeaders.SET_COOKIE));
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }

    /**
     * Proxy a POST request with cookies (for refresh/logout).
     */
    private ResponseEntity<?> proxyPostWithCookies(String path, Object body, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Forward cookies from original request
            String cookieHeader = request.getHeader("Cookie");
            if (cookieHeader != null) {
                headers.set("Cookie", cookieHeader);
            }
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.POST, entity, String.class
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            if (response.getHeaders().containsKey(HttpHeaders.SET_COOKIE)) {
                responseHeaders.addAll(HttpHeaders.SET_COOKIE, response.getHeaders().get(HttpHeaders.SET_COOKIE));
            }

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }

    /**
     * Proxy a GET request with Authorization header.
     */
    private ResponseEntity<?> proxyGetWithAuth(String path, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.GET, entity, String.class
            );

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
