package com.scopeflow.user.contract;

import com.scopeflow.user.adapter.in.web.auth.AuthController;
import com.scopeflow.user.adapter.in.web.user.UserController;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.domain.exception.DuplicateEmailException;
import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract tests (provider side).
 *
 * Sets up test data and mocks for contract verification.
 * All generated contract tests extend this class.
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

    @BeforeEach
    public void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);

        // Mock test user (ACTIVE)
        UserActive testUser = createTestUser();

        // Mock invited user (INACTIVE)
        UserInactive invitedUser = createInvitedUser();

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

        // Mock getUserByEmail for /users/by-email/{email}
        when(userService.getUserByEmail(new Email(TEST_EMAIL)))
                .thenReturn(Optional.of(testUser));

        when(userService.getUserByEmail(new Email(NOT_FOUND_EMAIL)))
                .thenReturn(Optional.empty());

        // ============ Invited user creation mocks ============

        // Mock duplicate email detection
        when(userService.getUserByEmail(new Email(INVITED_EMAIL)))
                .thenReturn(Optional.empty());

        // Mock invitedBy user exists
        when(userService.getUserById(new UserId(TEST_USER_ID)))
                .thenReturn(Optional.of(testUser));

        // Mock save invited user
        when(userService.saveInvitedUser(any(UserInactive.class)))
                .thenAnswer(invocation -> {
                    UserInactive user = invocation.getArgument(0);
                    return user;
                });
    }

    // ============ Helper methods ============

    private UserActive createTestUser() {
        UserId userId = new UserId(TEST_USER_ID);
        Email email = new Email(TEST_EMAIL);
        PasswordHash passwordHash = new PasswordHash("$2a$10$hashed_password_mock");
        Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");

        return new UserActive(userId, email, passwordHash, TEST_FULL_NAME, TEST_PHONE, createdAt, createdAt);
    }

    private UserInactive createInvitedUser() {
        UserId userId = new UserId(INVITED_USER_ID);
        Email email = new Email(INVITED_EMAIL);
        PasswordHash passwordHash = new PasswordHash("$2a$10$temp_password_mock");
        String displayName = "Invited";
        Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");

        return new UserInactive(userId, email, passwordHash, displayName, null, createdAt, createdAt);
    }
}
