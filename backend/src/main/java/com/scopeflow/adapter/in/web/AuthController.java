package com.scopeflow.adapter.in.web;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — Authentication & User Management.
 *
 * Endpoints: register, login, refresh token, logout, validate token.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  /**
   * POST /auth/register — Register new user.
   *
   * @param request RegisterRequest with email, password, fullName
   * @return AuthResponse with JWT token and user info
   */
  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
    // TODO: Implement registration logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /auth/login — Authenticate user.
   *
   * @param request LoginRequest with email and password
   * @return AuthResponse with JWT token and user info
   */
  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    // TODO: Implement login logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /auth/refresh — Refresh JWT token.
   *
   * @param request RefreshTokenRequest with refresh_token
   * @return new AuthResponse with refreshed JWT
   */
  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
    // TODO: Implement refresh token logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /auth/validate — Validate JWT token.
   *
   * @param request ValidateTokenRequest
   * @return TokenValidationResponse
   */
  @PostMapping("/validate")
  public ResponseEntity<TokenValidationResponse> validateToken(
      @RequestBody ValidateTokenRequest request) {
    // TODO: Implement token validation
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /auth/logout — Logout user (revoke token if needed).
   *
   * @return ResponseEntity with success message
   */
  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    // TODO: Implement logout logic (token revocation if needed)
    return ResponseEntity.ok().build();
  }

  // ============================================================================
  // DTOs
  // ============================================================================

  public record RegisterRequest(String email, String password, String fullName, String phone) {}

  public record LoginRequest(String email, String password) {}

  public record RefreshTokenRequest(String refreshToken) {}

  public record ValidateTokenRequest(String token) {}

  public record AuthResponse(
      UUID userId,
      String email,
      String fullName,
      String jwtToken,
      String refreshToken,
      long expiresIn) {}

  public record TokenValidationResponse(boolean valid, UUID userId, String email) {}
}
