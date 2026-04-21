package com.scopeflow.user.contract;

import com.scopeflow.user.adapter.in.web.auth.AuthController;
import com.scopeflow.user.adapter.in.web.user.UserController;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.application.usecase.*;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.PasswordHasher;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Spring Cloud Contract base class for User Service contracts.
 *
 * Setup:
 * - Standalone MockMvc (no full Spring context for speed)
 * - Mocked dependencies (repository, password hasher, JWT service)
 * - Consistent test data matching contract payloads
 *
 * Naming convention:
 * - All contract YAML files must be named with pattern: should{Action}.yml
 * - This base class is referenced by Spring Cloud Contract plugin
 */
@ExtendWith(MockitoExtension.class)
public class UserContractBase {

    // Test data constants (must match contract YAMLs)
    private static final UUID TEST_USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "ValidPassword123!";
    private static final String TEST_FULL_NAME = "Test User";
    private static final String TEST_PHONE = "+55119999988888";
    private static final String TEST_ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
    private static final String TEST_REFRESH_TOKEN = "refresh_token_example";
    private static final long ACCESS_TOKEN_EXPIRY = 900L; // 15 minutes
    private static final long REFRESH_TOKEN_EXPIRY = 604800; // 7 days

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private UserService userService;

    // Use cases
    private RegisterUserUseCase registerUserUseCase;
    private AuthenticateUserUseCase authenticateUserUseCase;
    private RefreshTokenUseCase refreshTokenUseCase;
    private InviteUserUseCase inviteUserUseCase;
    private BlockUserByIdUseCase blockUserByIdUseCase;

    // Controllers
    private AuthController authController;
    private UserController userController;

    @BeforeEach
    public void setup() {
        // Initialize use cases with mocked dependencies
        registerUserUseCase = new RegisterUserUseCase(userRepository, passwordHasher);
        authenticateUserUseCase = new AuthenticateUserUseCase(userRepository, passwordHasher, tokenIssuer);
        refreshTokenUseCase = new RefreshTokenUseCase(tokenIssuer, userRepository);
        inviteUserUseCase = new InviteUserUseCase(userRepository, passwordHasher);
        blockUserByIdUseCase = new BlockUserByIdUseCase(userRepository);

        // Initialize controllers
        authController = new AuthController(
                registerUserUseCase,
                authenticateUserUseCase,
                refreshTokenUseCase,
                tokenIssuer,
                userRepository
        );

        userController = new UserController(userService, inviteUserUseCase, blockUserByIdUseCase);

        // Setup default mocks
        setupUserRepositoryMocks();
        setupPasswordHasherMocks();
        setupTokenIssuerMocks();
        setupUserServiceMocks();

        // Configure RestAssured standalone setup (no Spring context)
        RestAssuredMockMvc.standaloneSetup(authController, userController);
    }

    private void setupUserRepositoryMocks() {
        UserActive testUser = new UserActive(
                new UserId(TEST_USER_ID),
                new Email(TEST_EMAIL),
                new PasswordHash("$2a$10$hashedPassword"),
                TEST_FULL_NAME,
                TEST_PHONE,
                null, // workspaceId
                Instant.parse("2025-01-15T10:30:00Z"),
                Instant.parse("2025-01-15T10:30:00Z")
        );

        // Mock: findByEmail returns test user
        when(userRepository.findByEmail(any(Email.class)))
                .thenReturn(Optional.of(testUser));

        // Mock: findById returns test user
        when(userRepository.findById(any(UserId.class)))
                .thenReturn(Optional.of(testUser));

        // Mock: existsByEmail returns false for new registrations
        when(userRepository.existsByEmail(any(Email.class)))
                .thenReturn(false);

        // Mock: save does nothing (void method)
        // No mock needed for void methods
    }

    private void setupPasswordHasherMocks() {
        // Mock: hash always returns a valid bcrypt hash
        when(passwordHasher.hash(anyString()))
                .thenReturn("$2a$10$hashedPassword");

        // Mock: matches always returns true (valid password)
        when(passwordHasher.matches(anyString(), anyString()))
                .thenReturn(true);
    }

    private void setupTokenIssuerMocks() {
        // Mock: issue access token
        when(tokenIssuer.issueAccessToken(any(UUID.class), anyString(), any(), anyString()))
                .thenReturn(TEST_ACCESS_TOKEN);

        // Mock: issue refresh token
        when(tokenIssuer.issueRefreshToken(any(UUID.class)))
                .thenReturn(TEST_REFRESH_TOKEN);

        // Mock: extract user ID from refresh token
        when(tokenIssuer.extractUserIdFromRefreshToken(anyString()))
                .thenReturn(TEST_USER_ID);

        // Mock: token expiration times
        when(tokenIssuer.accessTokenExpirationSeconds())
                .thenReturn(ACCESS_TOKEN_EXPIRY);

        when(tokenIssuer.refreshTokenExpirationSeconds())
                .thenReturn(REFRESH_TOKEN_EXPIRY);
    }

    private void setupUserServiceMocks() {
        UserActive testUser = new UserActive(
                new UserId(TEST_USER_ID),
                new Email(TEST_EMAIL),
                new PasswordHash("$2a$10$hashedPassword"),
                TEST_FULL_NAME,
                TEST_PHONE,
                null,
                Instant.parse("2025-01-15T10:30:00Z"),
                Instant.parse("2025-01-15T10:30:00Z")
        );

        // Mock: getUserByEmail
        when(userService.getUserByEmail(any(Email.class)))
                .thenReturn(Optional.of(testUser));

        // Mock: getUserById
        when(userService.getUserById(any(UserId.class)))
                .thenReturn(Optional.of(testUser));
    }
}
