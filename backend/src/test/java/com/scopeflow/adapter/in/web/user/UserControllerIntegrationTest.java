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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for UserController with Testcontainers.
 *
 * Validates security (auth filter, JWT), routing, and error handling end-to-end.
 * The RestTemplate (proxy to user-service) is mocked so tests do not require
 * a running user-service instance.
 *
 * Endpoints covered:
 * - GET /api/v1/users/by-email/{email}
 * - POST /api/v1/users/invited
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

    // Proxy to user-service is mocked — tests do not require a real user-service
    @MockBean
    private RestTemplate authServiceRestTemplate;

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
        @DisplayName("should return 200 proxied from user-service when user exists")
        void shouldReturnUser_whenEmailExists() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            String upstreamBody = """
                    {"id":"%s","email":"%s","fullName":"Test User","status":"ACTIVE",
                     "createdAt":"2024-01-01T00:00:00Z"}
                    """.formatted(user.getId(), TEST_EMAIL);

            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willReturn(ResponseEntity.ok(upstreamBody));

            mockMvc.perform(get("/api/v1/users/by-email/{email}", TEST_EMAIL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(user.getId().toString())))
                    .andExpect(jsonPath("$.email", is(TEST_EMAIL.toLowerCase())))
                    .andExpect(jsonPath("$.fullName", is("Test User")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("should forward 404 from user-service when email not found")
        void shouldReturn404_whenEmailNotFound() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            String problemJson = """
                    {"type":"https://api.scopeflow.com/errors/user-not-found",
                     "title":"User Not Found","status":404,"error_code":"USER-010",
                     "error_id":"test-id","timestamp":"2024-01-01T00:00:00Z"}
                    """;

            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), problemJson.getBytes(), null));

            mockMvc.perform(get("/api/v1/users/by-email/{email}", "nonexistent@example.com")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 401 when no Authorization header is present")
        void shouldReturn401_whenNoAuthToken() throws Exception {
            mockMvc.perform(get("/api/v1/users/by-email/{email}", TEST_EMAIL))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============ POST /api/v1/users/invited ============

    @Nested
    @DisplayName("POST /api/v1/users/invited")
    class CreateInvited {

        @Test
        @DisplayName("should forward 201 from user-service when valid request")
        void shouldCreateInvitedUser_whenValidRequest() throws Exception {
            JpaUser invitingUser = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(invitingUser.getId(), invitingUser.getEmail());

            String newUserEmail = "invited@example.com";
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    newUserEmail,
                    Role.MEMBER,
                    invitingUser.getId()
            );

            String upstreamBody = """
                    {"id":"%s","email":"%s","fullName":"Invited","status":"INACTIVE",
                     "createdAt":"2024-01-01T00:00:00Z"}
                    """.formatted(UUID.randomUUID(), newUserEmail);

            given(authServiceRestTemplate.exchange(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), eq(String.class)
            )).willReturn(ResponseEntity.status(HttpStatus.CREATED).body(upstreamBody));

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is(newUserEmail.toLowerCase())))
                    .andExpect(jsonPath("$.status", is("INACTIVE")))
                    .andExpect(jsonPath("$.id", notNullValue()));
        }

        @Test
        @DisplayName("should forward 409 from user-service when email already exists")
        void shouldReturn409_whenEmailAlreadyExists() throws Exception {
            JpaUser existingUser = createActiveUser("existing@example.com");
            String token = createAuthToken(existingUser.getId(), existingUser.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "existing@example.com",
                    Role.MEMBER,
                    existingUser.getId()
            );

            String problemJson = """
                    {"type":"https://api.scopeflow.com/errors/duplicate-email",
                     "title":"Duplicate Email","status":409,"error_code":"USER-011",
                     "error_id":"test-id","timestamp":"2024-01-01T00:00:00Z"}
                    """;

            given(authServiceRestTemplate.exchange(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), eq(String.class)
            )).willThrow(HttpClientErrorException.create(
                    HttpStatus.CONFLICT, "Conflict", new HttpHeaders(), problemJson.getBytes(), null));

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 from Bean Validation when email is blank (no proxy)")
        void shouldReturn400_whenEmailIsBlank() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "",
                    Role.MEMBER,
                    user.getId()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("VALIDATION-400")))
                    .andExpect(jsonPath("$.title", is("Validation Error")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/validation-error")));
        }
    }

    // ============ Helper Methods ============

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

    private String createAuthToken(UUID userId, String email) {
        JpaWorkspace workspace = createWorkspace(userId);
        return jwtService.generateAccessToken(userId, email, workspace.getId(), "OWNER");
    }
}
