package com.scopeflow.user.contract;

import com.scopeflow.user.adapter.in.web.auth.AuthController;
import com.scopeflow.user.adapter.in.web.user.UserController;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.domain.exception.DuplicateEmailException;
import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.exception.InvalidInvitedByUserException;
import com.scopeflow.user.domain.shared.InvalidValueObjectException;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.UserRepository;
import io.jsonwebtoken.Claims;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Base class for Spring Cloud Contract tests (provider side).
 *
 * Uses @WebMvcTest for fast, isolated controller tests without database.
 * RestAssuredMockMvc.basePath is empty because @WebMvcTest does not apply
 * server.servlet.context-path. The context-path (/api/v1) is handled in
 * production by the embedded server; contract tests verify controller behavior
 * at the path the DispatcherServlet sees (without the context-path prefix).
 *
 * Therefore: contracts use paths WITHOUT /api/v1 prefix (e.g., /auth/login)
 * Note: the actual production endpoint is /api/v1/auth/login — this is
 * documented in the contract description fields.
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({ContractVerifierSecurityConfig.class})
public abstract class ContractVerifierBase {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // Required by JwtAuthenticationFilter (@Component that is loaded in @WebMvcTest context)
    @MockBean
    private UserRepository userRepository;

    // Test data constants
    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "ValidPassword123!";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_PHONE = "+55119999988888";
    private static final String TEST_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private static final long ACCESS_TOKEN_EXPIRATION = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRATION = 604800L; // 7 days

    private static final UUID INVITED_USER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");
    private static final String INVITED_EMAIL = "invited@example.com";
    private static final String NOT_FOUND_EMAIL = "notfound@example.com";
    private static final String VALID_REFRESH_TOKEN =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJpYXQiOjE2NDI1MDAwMDAsImV4cCI6OTk5OTk5OTk5OX0" +
            ".refresh";
    private static final UUID UNKNOWN_INVITER_ID = UUID.fromString("999e8400-e29b-41d4-a716-446655440999");

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        // No basePath: @WebMvcTest sees paths without context-path prefix
        // Contracts must use paths without /api/v1

        // Mock passwordEncoder.encode for register and createInvited endpoints
        when(passwordEncoder.encode(any(CharSequence.class)))
                .thenReturn("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");

        // Mock test user (ACTIVE)
        UserActive testUser = createTestUser();

        // Mock invited user (INACTIVE)
        UserInactive invitedUser = createInvitedUser();

        // ============ JwtAuthenticationFilter mocks ============

        // Contract Bearer token for authenticated endpoints — register FIRST (specific before generic)
        // JwtAuthenticationFilter needs Claims to populate SecurityContext for /auth/me and /users/*
        String contractBearerToken =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                ".eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6InRlc3RAZXhhbXBsZS5jb20iLCJpYXQiOjE2NDI1MDAwMDAsImV4cCI6OTk5OTk5OTk5OX0.test";

        Claims mockClaims = mock(Claims.class);
        when(mockClaims.getSubject()).thenReturn(TEST_USER_ID.toString());
        when(mockClaims.get("email", String.class)).thenReturn(TEST_EMAIL);
        when(mockClaims.get("type", String.class)).thenReturn("access");
        when(mockClaims.get("workspace_id", String.class)).thenReturn(null);
        when(mockClaims.get("role", String.class)).thenReturn("OWNER");

        when(jwtService.validateAndExtract(contractBearerToken)).thenReturn(mockClaims);
        // All other tokens: return null → filter skips authentication (null check added to filter)

        // UserRepository: filter verifies user is ACTIVE after Claims extraction
        when(userRepository.findById(new UserId(TEST_USER_ID)))
                .thenReturn(Optional.of(testUser));

        // ============ Auth mocks ============

        // Mock successful login
        when(userService.getUserByEmail(new Email(TEST_EMAIL)))
                .thenReturn(Optional.of(testUser));

        when(passwordEncoder.matches(TEST_PASSWORD, testUser.getPasswordHash().value()))
                .thenReturn(true);

        when(passwordEncoder.matches("WrongPassword", testUser.getPasswordHash().value()))
                .thenReturn(false);

        // Mock JWT generation
        when(jwtService.generateAccessToken(TEST_USER_ID, TEST_EMAIL, null, null))
                .thenReturn(TEST_JWT_TOKEN);

        when(jwtService.generateRefreshToken(TEST_USER_ID))
                .thenReturn("refresh_token_mock");

        when(jwtService.getAccessTokenExpirationMs())
                .thenReturn(ACCESS_TOKEN_EXPIRATION * 1000);

        when(jwtService.getRefreshTokenExpirationMs())
                .thenReturn(REFRESH_TOKEN_EXPIRATION * 1000);

        // Mock JWT extraction for /auth/me
        when(jwtService.extractUserId(any()))
                .thenReturn(TEST_USER_ID);

        // Mock getUserById for authenticated endpoints
        when(userService.getUserById(new UserId(TEST_USER_ID)))
                .thenReturn(Optional.of(testUser));

        // ============ User lookup mocks ============

        when(userService.getUserByEmail(new Email(NOT_FOUND_EMAIL)))
                .thenReturn(Optional.empty());

        // ============ Invited user creation mocks ============

        // invited@example.com is available (not registered)
        when(userService.getUserByEmail(new Email(INVITED_EMAIL)))
                .thenReturn(Optional.empty());

        // Mock save invited user (void method)
        doNothing().when(userService).saveInvitedUser(any(UserInactive.class));

        // ============ Register mocks ============

        when(userService.registerUser(
                eq(new Email("newuser@example.com")), any(), any(), any()))
                .thenReturn(testUser);

        doThrow(new EmailAlreadyRegisteredException("Email already registered: test@example.com"))
                .when(userService).registerUser(eq(new Email(TEST_EMAIL)), any(), any(), any());

        // ============ InvalidValueObjectException mocks (VO-001) ============

        // Invalid email format: "invalid-email" (no @)
        doThrow(new InvalidValueObjectException("VO-001", "Invalid email format: invalid-email"))
                .when(userService).registerUser(eq(new Email("invalid-email")), any(), any(), any());

        // Invalid email format: "user@" (missing domain)
        doThrow(new InvalidValueObjectException("VO-001", "Invalid email format: user@"))
                .when(userService).registerUser(eq(new Email("user@")), any(), any(), any());

        // Invalid email format: "" (blank)
        doThrow(new InvalidValueObjectException("VO-001", "Invalid email format: "))
                .when(userService).registerUser(eq(new Email("")), any(), any(), any());

        // ============ Refresh token mocks ============

        when(jwtService.isRefreshToken(VALID_REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(VALID_REFRESH_TOKEN)).thenReturn(TEST_USER_ID);

        when(jwtService.isRefreshToken(null)).thenReturn(false);
        when(jwtService.isRefreshToken("")).thenReturn(false);

        // ============ Invalid inviter and role mocks ============

        when(userService.getUserById(new UserId(UNKNOWN_INVITER_ID)))
                .thenReturn(Optional.empty());

        when(userService.getUserByEmail(new Email("newmember@example.com")))
                .thenReturn(Optional.empty());

        when(userService.getUserByEmail(new Email("owner@example.com")))
                .thenReturn(Optional.empty());
    }

    // ============ Helper methods ============

    private UserActive createTestUser() {
        UserId userId = new UserId(TEST_USER_ID);
        Email email = new Email(TEST_EMAIL);
        // Valid bcrypt hash (12 rounds, satisfies PasswordHash format validation)
        PasswordHash passwordHash = new PasswordHash("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");

        return new UserActive(userId, email, passwordHash, TEST_FULL_NAME, TEST_PHONE, createdAt, createdAt);
    }

    private UserInactive createInvitedUser() {
        UserId userId = new UserId(INVITED_USER_ID);
        Email email = new Email(INVITED_EMAIL);
        PasswordHash passwordHash = new PasswordHash("$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        String displayName = "Invited";
        Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");

        return new UserInactive(userId, email, passwordHash, displayName, null, createdAt, createdAt);
    }
}
