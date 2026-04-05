package com.scopeflow.user.adapter.in.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.user.adapter.out.persistence.JpaUser;
import com.scopeflow.user.adapter.out.persistence.JpaUserSpringRepository;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.domain.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
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
 * Integration tests for UserController with Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
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
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JpaUserSpringRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("GET /api/v1/users/by-email/{email}")
    class GetByEmail {

        @Test
        @DisplayName("should return 200 with user when email exists")
        void shouldReturnUser_whenEmailExists() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            mockMvc.perform(get("/api/v1/users/by-email/{email}", TEST_EMAIL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(user.getId().toString())))
                    .andExpect(jsonPath("$.email", is(TEST_EMAIL.toLowerCase())))
                    .andExpect(jsonPath("$.fullName", is("Test User")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @DisplayName("should return 404 when email not found")
        void shouldReturn404_whenEmailNotFound() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            mockMvc.perform(get("/api/v1/users/by-email/{email}", "nonexistent@example.com")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code", is("USER-010")))
                    .andExpect(jsonPath("$.title", is("User Not Found")));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/invited")
    class CreateInvited {

        @Test
        @DisplayName("should return 201 and create invited user when valid request")
        void shouldCreateInvitedUser_whenValidRequest() throws Exception {
            JpaUser invitingUser = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(invitingUser.getId(), invitingUser.getEmail());

            String newUserEmail = "invited@example.com";
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    newUserEmail, Role.MEMBER, invitingUser.getId()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is(newUserEmail.toLowerCase())))
                    .andExpect(jsonPath("$.status", is("INACTIVE")))
                    .andExpect(jsonPath("$.id", notNullValue()));

            List<JpaUser> allUsers = userRepository.findAll();
            assertThat(allUsers).hasSize(2);
        }

        @Test
        @DisplayName("should return 409 when email already exists")
        void shouldReturn409_whenEmailAlreadyExists() throws Exception {
            JpaUser existingUser = createActiveUser("existing@example.com");
            String token = createAuthToken(existingUser.getId(), existingUser.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "existing@example.com", Role.MEMBER, existingUser.getId()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code", is("USER-011")));
        }

        @Test
        @DisplayName("should return 400 when invitedBy user does not exist")
        void shouldReturn400_whenInvitedByUserNotFound() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "invited@example.com", Role.MEMBER, UUID.randomUUID()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-012")));
        }

        @Test
        @DisplayName("should return 400 when role is OWNER")
        void shouldReturn400_whenRoleIsOwner() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = createAuthToken(user.getId(), user.getEmail());

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "invited@example.com", Role.OWNER, user.getId()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-013")));
        }
    }

    // ============ Helper Methods ============

    private JpaUser createActiveUser(String email) {
        JpaUser user = new JpaUser(
                UUID.randomUUID(), email, TEST_PASSWORD_HASH, "Test User", null,
                "ACTIVE", Instant.now(), Instant.now()
        );
        return userRepository.save(user);
    }

    private String createAuthToken(UUID userId, String email) {
        return jwtService.generateAccessToken(userId, email, null, "OWNER");
    }
}
