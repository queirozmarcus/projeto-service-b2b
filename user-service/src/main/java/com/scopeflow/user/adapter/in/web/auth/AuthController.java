package com.scopeflow.user.adapter.in.web.auth;

import com.scopeflow.user.adapter.in.web.auth.dto.*;
import com.scopeflow.user.application.usecase.AuthenticateUserUseCase;
import com.scopeflow.user.application.usecase.RefreshTokenUseCase;
import com.scopeflow.user.application.usecase.RegisterUserUseCase;
import com.scopeflow.user.config.SecurityUtil;
import com.scopeflow.user.domain.model.User;
import com.scopeflow.user.domain.model.UserId;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;
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
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.UUID;

/**
 * Auth controller: registration, login, token refresh, profile, logout.
 *
 * Path: /api/v1/auth
 * Public endpoints: register, login, refresh, logout
 * Protected: /me
 *
 * Security model:
 * - Access token: short-lived (15min), returned in response body
 * - Refresh token: long-lived (7d), delivered via httpOnly Set-Cookie
 *
 * Controller responsibility: HTTP translation only.
 * Business logic lives in RegisterUserUseCase, AuthenticateUserUseCase, RefreshTokenUseCase.
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

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final TokenIssuer tokenIssuer;
    private final UserRepository userRepository;

    public AuthController(RegisterUserUseCase registerUserUseCase,
                          AuthenticateUserUseCase authenticateUserUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          TokenIssuer tokenIssuer,
                          UserRepository userRepository) {
        this.registerUserUseCase = registerUserUseCase;
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.tokenIssuer = tokenIssuer;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @RateLimit
    @Operation(summary = "Register new user account")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        var user = registerUserUseCase.execute(
                request.email(), request.password(), request.fullName(), request.phone());

        log.info("User registered: userId={}, email={}", user.getId().value(), user.getEmail().normalized());

        return buildLoginResponse(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @RateLimit
    @Operation(summary = "Authenticate and obtain tokens")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var result = authenticateUserUseCase.execute(request.email(), request.password());

        log.info("User logged in: userId={}", result.user().getId().value());

        return buildLoginResponse(result.user(), HttpStatus.OK);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, REFRESH_TOKEN_COOKIE);

        var result = refreshTokenUseCase.execute(refreshToken);

        log.info("Access token refreshed");

        return ResponseEntity.ok(new AccessTokenResponse(result.accessToken(), result.expiresInSeconds()));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public UserResponse me() {
        UUID userId = SecurityUtil.getUserId();
        User user = userRepository.findById(new UserId(userId))
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
        String accessToken = tokenIssuer.issueAccessToken(
                user.getId().value(), user.getEmail().normalized(), "USER");
        String refreshToken = tokenIssuer.issueRefreshToken(user.getId().value());

        ResponseCookie cookie = ResponseCookie
                .from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(tokenIssuer.refreshTokenExpirationSeconds())
                .build();

        LoginResponse body = new LoginResponse(
                accessToken,
                tokenIssuer.accessTokenExpirationSeconds(),
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
