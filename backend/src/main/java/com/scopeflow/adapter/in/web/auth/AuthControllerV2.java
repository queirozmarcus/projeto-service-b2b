package com.scopeflow.adapter.in.web.auth;

import com.scopeflow.adapter.in.web.auth.dto.*;
import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.config.JwtService;
import com.scopeflow.core.domain.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

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
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerV2.class);
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    // Path restrito: cookie enviado apenas para o endpoint de refresh.
    // Inclui o context-path configurado em server.servlet.context-path (/api/v1).
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/refresh";

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthControllerV2(
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * POST /auth/register
     * Register a new user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/register")
    @Operation(summary = "Register new user account")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        Email email = new Email(request.email());
        PasswordHash hash = new PasswordHash(passwordEncoder.encode(request.password()));

        UserActive user = userService.registerUser(email, hash, request.fullName(), request.phone());

        log.info("User registered: userId={}, email={}", user.getId().value(), email.normalized());

        return buildLoginResponse(user, HttpStatus.CREATED);
    }

    /**
     * POST /auth/login
     * Authenticate user. Returns access token in body; refresh token via httpOnly cookie.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Email email = new Email(request.email());

        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash().value())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        log.info("User logged in: userId={}", user.getId().value());

        return buildLoginResponse(user, HttpStatus.OK);
    }

    /**
     * POST /auth/refresh
     * Exchange refresh token (from httpOnly cookie) for a new access token.
     * The cookie is read automatically by the browser — no body param needed.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null || !jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token inválido ou expirado. Faça login novamente.");
        }

        UUID userId = jwtService.extractUserId(refreshToken);

        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getId().value(),
                user.getEmail().normalized(),
                null,
                null
        );

        log.info("Access token refreshed: userId={}", userId);

        return ResponseEntity.ok(new AccessTokenResponse(
                newAccessToken,
                jwtService.getAccessTokenExpirationMs() / 1000
        ));
    }

    /**
     * GET /auth/me
     * Return authenticated user's profile.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserResponse me() {
        UUID userId = SecurityUtil.getUserId();
        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return UserResponse.from(user);
    }

    /**
     * POST /auth/logout
     * Clears the refresh token cookie. Client discards the access token from memory.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout: clears refresh token cookie")
    public ResponseEntity<Void> logout() {
        log.info("User logged out: userId={}", SecurityUtil.getUserId());

        // Invalidate cookie by setting max-age=0
        ResponseCookie clearCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    // ============ Private helpers ============

    /**
     * Build login/register response: access token in body + refresh token as httpOnly cookie.
     */
    private ResponseEntity<LoginResponse> buildLoginResponse(User user, HttpStatus status) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().value(),
                user.getEmail().normalized(),
                null,
                null
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId().value());

        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(jwtService.getRefreshTokenExpirationMs() / 1000)
                .build();

        LoginResponse body = new LoginResponse(
                accessToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName()
        );

        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }

    /**
     * Extract a cookie value by name from the request.
     */
    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
