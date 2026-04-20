package com.scopeflow.user.adapter.in.web.auth;

import com.scopeflow.user.adapter.in.web.auth.dto.*;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.config.SecurityUtil;
import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

/**
 * Auth controller: registration, login, token refresh, profile, logout.
 *
 * Path: /auth (under context-path /api/v1)
 * Public endpoints: register, login, refresh, logout
 * Protected: /me
 *
 * Security model:
 * - Access token: short-lived (15min), returned in response body
 * - Refresh token: long-lived (7d), delivered via httpOnly Set-Cookie
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/";

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @RateLimit
    @Operation(summary = "Register new user account")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        Email email = new Email(request.email());
        PasswordHash hash = new PasswordHash(passwordEncoder.encode(request.password()));

        UserActive user = userService.registerUser(email, hash, request.fullName(), request.phone());

        log.info("User registered: userId={}, email={}", user.getId().value(), email.normalized());

        return buildLoginResponse(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @RateLimit
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

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);

        if (refreshToken == null || !jwtService.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token invalido ou expirado. Faca login novamente.");
        }

        UUID userId = jwtService.extractUserId(refreshToken);

        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        String newAccessToken = jwtService.generateAccessToken(
                user.getId().value(), user.getEmail().normalized(), null, null
        );

        log.info("Access token refreshed: userId={}", userId);

        return ResponseEntity.ok(new AccessTokenResponse(
                newAccessToken, jwtService.getAccessTokenExpirationMs() / 1000
        ));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserResponse me() {
        UUID userId = SecurityUtil.getUserId();
        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return UserResponse.from(user);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout: clears refresh token cookie")
    public ResponseEntity<Void> logout() {
        try {
            log.info("User logged out: userId={}", SecurityUtil.getUserId());
        } catch (Exception e) {
            log.info("Logout called without active session (cookie-only logout)");
        }

        ResponseCookie clearCookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .build();
    }

    // ============ Private helpers ============

    private ResponseEntity<LoginResponse> buildLoginResponse(User user, HttpStatus status) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().value(), user.getEmail().normalized(), null, null
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId().value());

        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
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

    private String extractCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(c -> name.equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        // Fallback: parse raw Cookie header (used in test environments where getCookies()
        // may not reflect headers set directly — e.g. Spring MockMvc via RestAssured)
        String cookieHeader = request.getHeader(HttpHeaders.COOKIE);
        if (cookieHeader == null) return null;
        return Arrays.stream(cookieHeader.split(";"))
                .map(String::trim)
                .filter(part -> part.startsWith(name + "="))
                .map(part -> part.substring(name.length() + 1))
                .findFirst()
                .orElse(null);
    }
}
