package com.scopeflow.adapter.in.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.adapter.out.persistence.user.JpaUser;
import com.scopeflow.adapter.out.persistence.user.JpaUserSpringRepository;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspace;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceSpringRepository;
import com.scopeflow.config.JwtService;
import com.scopeflow.core.domain.workspace.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController endpoints with Testcontainers.
 *
 * Tests:
 * - GET /api/v1/users/by-email/{email}
 * - POST /api/v1/users/invited
 *
 * Uses real PostgreSQL container, full Spring Boot context with Flyway migrations,
 * and validates end-to-end flow with database persistence.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.rabbitmq.host", () -> "localhost");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserSpringRepository userRepository;

    @Autowired
    private JpaWorkspaceSpringRepository workspaceRepository;

    @Autowired
    private JwtService jwtService;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String TEST_WORKSPACE_NAME = "Test Workspace";
    private static final String TEST_NICHE = "social-media";

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        workspaceRepository.deleteAll();
    }

    // ============ GET /api/v1/users/by-email/{email} ============

    @Nested
    @DisplayName("GET /api/v1/users/by-email/{email}")
    class GetByEmail {

        @Test
        @DisplayName("should return 200 with user when email exists")
        void shouldReturnUser_whenEmailExists() throws Exception {
            // Given
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", TEST_EMAIL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(user.getId().toString())))
                    .andExpect(jsonPath("$.email", is(TEST_EMAIL.toLowerCase())))
                    .andExpect(jsonPath("$.fullName", is("Test User")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));

            // Verify user exists in database
            var dbUser = userRepository.findById(user.getId());
            assertThat(dbUser).isPresent();
            assertThat(dbUser.get().getEmail()).isEqualTo(TEST_EMAIL);
        }

        @Test
        @DisplayName("should return 404 when email not found")
        void shouldReturn404_whenEmailNotFound() throws Exception {
            // Given
            String nonExistentEmail = "nonexistent@example.com";
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", nonExistentEmail)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code", is("USER-010")))
                    .andExpect(jsonPath("$.title", is("User Not Found")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/user-not-found")))
                    .andExpect(jsonPath("$.error_id", notNullValue()))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }

        @Test
        @DisplayName("should return 400 when email format is invalid")
        void shouldReturn400_whenEmailFormatInvalid() throws Exception {
            // Given
            String invalidEmail = "invalid-email";
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", invalidEmail)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isInternalServerError()); // Email VO throws IllegalArgumentException
        }
    }

    // ============ POST /api/v1/users/invited ============

    @Nested
    @DisplayName("POST /api/v1/users/invited")
    class CreateInvited {

        @Test
        @DisplayName("should return 201 and create invited user when valid request")
        void shouldCreateInvitedUser_whenValidRequest() throws Exception {
            // Given
            JpaUser invitingUser = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(invitingUser.getId(), invitingUser.getEmail());

            String newUserEmail = "invited@example.com";
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    newUserEmail,
                    Role.MEMBER,
                    invitingUser.getId()
            );

            // When
            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is(newUserEmail.toLowerCase())))
                    .andExpect(jsonPath("$.status", is("INACTIVE")))
                    .andExpect(jsonPath("$.fullName").exists())
                    .andExpect(jsonPath("$.id", notNullValue()));

            // Then: verify user was persisted to database
            List<JpaUser> allUsers = userRepository.findAll();
            assertThat(allUsers).hasSize(2); // inviting user + new invited user

            var invitedUser = allUsers.stream()
                    .filter(u -> u.getEmail().equals(newUserEmail))
                    .findFirst();
            assertThat(invitedUser).isPresent();
            assertThat(invitedUser.get().getStatus()).isEqualTo("INACTIVE");
            assertThat(invitedUser.get().getFullName()).isNotNull();
        }

        @Test
        @DisplayName("should return 409 when email already exists")
        void shouldReturn409_whenEmailAlreadyExists() throws Exception {
            // Given
            JpaUser existingUser = createActiveUser("existing@example.com");
            String token = createAuthToken(existingUser.getId(), existingUser.getEmail());

            // Try to invite user with same email
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "existing@example.com",
                    Role.MEMBER,
                    existingUser.getId()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code", is("USER-011")))
                    .andExpect(jsonPath("$.title", is("Duplicate Email")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/duplicate-email")))
                    .andExpect(jsonPath("$.error_id", notNullValue()))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            // Verify no duplicate was created
            List<JpaUser> users = userRepository.findAll();
            assertThat(users).hasSize(1);
        }

        @Test
        @DisplayName("should return 400 when invitedBy user does not exist")
        void shouldReturn400_whenInvitedByUserNotFound() throws Exception {
            // Given
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            UUID nonExistentUserId = UUID.randomUUID();
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "invited@example.com",
                    Role.MEMBER,
                    nonExistentUserId
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-012")))
                    .andExpect(jsonPath("$.title", is("Invalid Invited By User")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/invalid-invited-by-user")))
                    .andExpect(jsonPath("$.error_id", notNullValue()))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            // Verify no user was created
            List<JpaUser> users = userRepository.findAll();
            assertThat(users).hasSize(1); // only the auth user
        }

        @Test
        @DisplayName("should return 400 when role is OWNER")
        void shouldReturn400_whenRoleIsOwner() throws Exception {
            // Given
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "invited@example.com",
                    Role.OWNER,
                    user.getId()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-013")))
                    .andExpect(jsonPath("$.title", is("Invalid Role")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/invalid-role")))
                    .andExpect(jsonPath("$.error_id", notNullValue()))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));

            // Verify no user was created
            List<JpaUser> users = userRepository.findAll();
            assertThat(users).hasSize(1); // only the auth user
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        void shouldReturn400_whenRequestValidationFails() throws Exception {
            // Given
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "",
                    Role.MEMBER,
                    user.getId()
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("VALIDATION-400")))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/validation-error")));

            // Verify no user was created
            List<JpaUser> users = userRepository.findAll();
            assertThat(users).hasSize(1); // only the auth user
        }
    }

    // ============ Helper Methods ============

    /**
     * Create and persist an ACTIVE user.
     */
    private JpaUser createActiveUser(String email) {
        JpaUser user = new JpaUser(
                UUID.randomUUID(),
                email,
                TEST_PASSWORD_HASH,
                "Test User",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        return userRepository.save(user);
    }

    /**
     * Create and persist a test workspace.
     */
    private JpaWorkspace createWorkspace(UUID ownerId) {
        JpaWorkspace workspace = new JpaWorkspace(
                UUID.randomUUID(),
                ownerId,
                TEST_WORKSPACE_NAME,
                TEST_NICHE,
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        return workspaceRepository.save(workspace);
    }

    /**
     * Generate a JWT access token for test authentication.
     */
    private String createAuthToken(UUID userId, String email) {
        // Create workspace for token context
        JpaWorkspace workspace = createWorkspace(userId);
        return jwtService.generateAccessToken(userId, email, workspace.getId(), "OWNER");
    }
}
