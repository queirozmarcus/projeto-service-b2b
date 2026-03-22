package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication endpoints for ScopeFlow API (v1).
 *
 * Handles:
 * - User registration (email + password)
 * - Login (returns JWT access + refresh tokens)
 * - Token refresh
 * - Token validation
 * - Logout (token revocation)
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthControllerV1 {

    /**
     * Register a new user.
     *
     * Request example:
     * ```json
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePassword123!",
     *   "full_name": "John Doe",
     *   "phone": "+5511999999999"
     * }
     * ```
     *
     * Response: JWT tokens (access + refresh)
     * Status: 201 CREATED
     *
     * Invariant: Email must be unique.
     * Error: 409 CONFLICT if email already registered
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Create a new user account with email and password. Returns JWT tokens."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already registered (USER-001)"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input (email, password validation)"
            )
    })
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        throw new UnsupportedOperationException("Implement in adapter layer with use case delegation");
    }

    /**
     * Authenticate user (login).
     *
     * Request example:
     * ```json
     * {
     *   "email": "user@example.com",
     *   "password": "SecurePassword123!"
     * }
     * ```
     *
     * Response: JWT tokens (access + refresh)
     * Status: 200 OK
     *
     * Error: 401 UNAUTHORIZED if email/password invalid
     */
    @PostMapping("/login")
    @Operation(
            summary = "Authenticate user (login)",
            description = "Login with email and password. Returns JWT access and refresh tokens."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User account inactive or deleted"
            )
    })
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        throw new UnsupportedOperationException("Implement in adapter layer with use case delegation");
    }

    /**
     * Refresh access token using refresh token.
     *
     * Request example:
     * ```json
     * {
     *   "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * ```
     *
     * Response: New access token (+ optional new refresh token)
     * Status: 200 OK
     *
     * Error: 401 UNAUTHORIZED if refresh token expired/invalid
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Use refresh token to obtain new access token (15 min expiry)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Refresh token expired or invalid"
            )
    })
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        throw new UnsupportedOperationException("Implement in adapter layer with use case delegation");
    }

    /**
     * Validate access token.
     *
     * Used by client to check if token is still valid before making requests.
     *
     * Response: HTTP 200 (valid), 401 (invalid)
     */
    @PostMapping("/validate")
    @Operation(
            summary = "Validate access token",
            description = "Check if access token is valid and not expired."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token invalid or expired")
    })
    public ResponseEntity<Void> validate() {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Logout (revoke token).
     *
     * Used by client to explicitly logout and invalidate tokens.
     * After logout, token is added to blacklist/revocation list.
     *
     * Response: HTTP 204 NO_CONTENT
     * Requires: Authorization: Bearer <token>
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Logout (revoke token)",
            description = "Logout and invalidate current JWT token."
    )
    @ApiResponse(responseCode = "204", description = "Logout successful")
    public ResponseEntity<Void> logout() {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    // ============ DTOs ============

    public record RegisterRequest(
            String email,
            String password,
            String full_name,
            String phone
    ) {
    }

    public record LoginRequest(
            String email,
            String password
    ) {
    }

    public record RefreshRequest(
            String refresh_token
    ) {
    }

    public record AuthResponse(
            String access_token,
            String refresh_token,
            int expires_in,
            String token_type,
            String user_id,
            String email
    ) {
    }
}
