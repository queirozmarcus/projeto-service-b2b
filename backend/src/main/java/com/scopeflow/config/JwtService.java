package com.scopeflow.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Stateless JWT token generation and validation.
 *
 * Access token claims: sub (userId), email, workspace_id, roles.
 * Refresh token claims: sub (userId), type=refresh.
 */
@Component
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:900000}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    /**
     * Generate access JWT token for authenticated user.
     */
    public String generateAccessToken(UUID userId, String email, UUID workspaceId, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("workspace_id", workspaceId != null ? workspaceId.toString() : null)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Generate refresh token (minimal claims — only userId).
     */
    public String generateRefreshToken(UUID userId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and extract all claims.
     *
     * @throws JwtException if token is invalid or expired
     */
    public Claims validateAndExtract(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract userId (subject) from token without full validation.
     */
    public UUID extractUserId(String token) {
        Claims claims = validateAndExtract(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Check if token is a refresh token.
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateAndExtract(token);
            return "refresh".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Validate token without throwing — returns true if valid.
     */
    public boolean isValid(String token) {
        try {
            validateAndExtract(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }
}
