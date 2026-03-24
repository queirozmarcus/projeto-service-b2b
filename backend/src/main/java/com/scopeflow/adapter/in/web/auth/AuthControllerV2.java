package com.scopeflow.adapter.in.web.auth;

import com.scopeflow.adapter.in.web.auth.dto.*;
import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.config.JwtService;
import com.scopeflow.core.domain.user.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Auth controller: registration, login, token refresh, profile.
 *
 * Path: /api/v1/auth
 * Public endpoints: register, login, refresh
 * Protected: /me, /logout
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration, login, and token management")
public class AuthControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerV2.class);

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
     * Register a new user. Returns access + refresh tokens on success.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register new user account")
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        Email email = new Email(request.email());
        PasswordHash hash = new PasswordHash(passwordEncoder.encode(request.password()));

        UserActive user = userService.registerUser(email, hash, request.fullName(), request.phone());

        log.info("User registered: userId={}, email={}", user.getId().value(), email.normalized());

        return buildTokenResponse(user, null);
    }

    /**
     * POST /auth/login
     * Authenticate user. Returns access + refresh tokens on success.
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain tokens")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
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

        return buildTokenResponse(user, null);
    }

    /**
     * POST /auth/refresh
     * Exchange refresh token for a new access token.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        if (!jwtService.isRefreshToken(request.refreshToken())) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(request.refreshToken());

        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.canLogin()) {
            throw new InvalidCredentialsException("Account is not active");
        }

        return buildTokenResponse(user, null);
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
     * Stateless JWT: no server-side state to clear. Client discards token.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Logout (client-side token discard)")
    public void logout() {
        log.info("User logged out: userId={}", SecurityUtil.getUserId());
    }

    // ============ Private helpers ============

    private TokenResponse buildTokenResponse(User user, UUID workspaceId) {
        String accessToken = jwtService.generateAccessToken(
                user.getId().value(),
                user.getEmail().normalized(),
                workspaceId,
                null // Role resolved after workspace context is set
        );
        String refreshToken = jwtService.generateRefreshToken(user.getId().value());

        return new TokenResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs() / 1000,
                user.getId().value(),
                user.getEmail().value(),
                user.getFullName()
        );
    }
}
