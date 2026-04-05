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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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
    // Path "/" garante que o browser inclua o cookie em TODAS as requisições,
    // permitindo que o Next.js middleware leia o refreshToken para proteger rotas.
    // SameSite=Lax é compatível com navegação direta (GET) e formulários (POST)
    // sem expor o cookie em requisições cross-site de terceiros.
    private static final String REFRESH_COOKIE_PATH = "/";

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    /**
     * Strangler Fig feature flag: when true, proxies auth requests to user-service.
     * Rollback: set to false and restart -- monolith handles auth locally.
     */
    @Value("${auth.service.use-extracted:false}")
    private boolean useExtractedAuthService;

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate authServiceRestTemplate;

    public AuthControllerV2(
            UserService userService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RestTemplate authServiceRestTemplate
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authServiceRestTemplate = authServiceRestTemplate;
    }

    /**
     * POST /auth/register
     * Register a new user. Returns access token in body; refresh token via httpOnly cookie.
     *
     * Strangler Fig: when auth.service.use-extracted=true, proxies to user-service.
     */
    @PostMapping("/register")
    @RateLimit
    @Operation(summary = "Register new user account")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying register to user-service: email={}", request.email());
            return proxyPost("/auth/register", request);
        }

        Email email = new Email(request.email());
        PasswordHash hash = new PasswordHash(passwordEncoder.encode(request.password()));

        UserActive user = userService.registerUser(email, hash, request.fullName(), request.phone());

        log.info("User registered: userId={}, email={}", user.getId().value(), email.normalized());

        return buildLoginResponse(user, HttpStatus.CREATED);
    }

    /**
     * POST /auth/login
     * Authenticate user. Returns access token in body; refresh token via httpOnly cookie.
     *
     * Strangler Fig: when auth.service.use-extracted=true, proxies to user-service.
     */
    @PostMapping("/login")
    @RateLimit
    @Operation(summary = "Authenticate and obtain tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying login to user-service: email={}", request.email());
            return proxyPost("/auth/login", request);
        }

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
     *
     * Strangler Fig: when auth.service.use-extracted=true, proxies to user-service.
     */
    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using httpOnly cookie")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying refresh to user-service");
            return proxyPostWithCookies("/auth/refresh", null, request);
        }

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
     *
     * Strangler Fig: when auth.service.use-extracted=true, proxies to user-service.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<?> me(HttpServletRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying /me to user-service");
            return proxyGetWithAuth("/auth/me", request);
        }

        UUID userId = SecurityUtil.getUserId();
        User user = userService.getUserById(new UserId(userId))
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * POST /auth/logout
     * Clears the refresh token cookie. Client discards the access token from memory.
     *
     * Strangler Fig: when auth.service.use-extracted=true, proxies to user-service.
     */
    @PostMapping("/logout")
    @Operation(summary = "Logout: clears refresh token cookie")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying logout to user-service");
            return proxyPostWithCookies("/auth/logout", null, request);
        }

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

    // ============ Strangler Fig: proxy helpers ============

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
            log.warn("[StranglerFig] user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[StranglerFig] Failed to proxy to user-service, falling back to monolith", e);
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
            log.warn("[StranglerFig] user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[StranglerFig] Failed to proxy to user-service, falling back to monolith", e);
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
            log.warn("[StranglerFig] user-service returned error: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[StranglerFig] Failed to proxy to user-service, falling back to monolith", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
