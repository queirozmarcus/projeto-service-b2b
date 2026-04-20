package com.scopeflow.user.adapter.in.web.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.auth.dto.LoginRequest;
import com.scopeflow.user.adapter.in.web.auth.dto.RegisterRequest;
import com.scopeflow.user.adapter.out.persistence.JpaUser;
import com.scopeflow.user.adapter.out.persistence.JpaUserSpringRepository;
import com.scopeflow.user.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController with Testcontainers PostgreSQL.
 *
 * Validates full flow: register -> login -> /me with real database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

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

    // MockMvc é reconstruído no @BeforeEach com contextPath explícito.
    // @AutoConfigureMockMvc sozinho não propaga server.servlet.context-path=/api/v1
    // para o Spring Security no MOCK web environment — sem isso, requestMatchers
    // como permitAll("/auth/login") não correspondem a "/auth/login".
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserSpringRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String TEST_PASSWORD = "Password1!";
    private static final String TEST_EMAIL = "test@example.com";

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class Register {

        @Test
        @DisplayName("should register user and return 201 with JWT")
        void shouldRegisterUser_andReturnJwt() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL, TEST_PASSWORD, "Test User", null
            );

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.userId", notNullValue()))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.fullName").value("Test User"))
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("refreshToken=")));

            // Verify user persisted to database
            var users = userRepository.findAll();
            assertThat(users).hasSize(1);
            assertThat(users.get(0).getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(users.get(0).getStatus()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should return 400 with violations when request body is invalid")
        void shouldReturn400_whenRequestBodyIsInvalid() throws Exception {
            // blank email and blank password → @NotBlank @Email violations
            RegisterRequest request = new RegisterRequest("", "", "Name", null);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations").isArray())
                    .andExpect(jsonPath("$.violations.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        }

        @Test
        @DisplayName("should return 409 when email already registered")
        void shouldReturn409_whenEmailExists() throws Exception {
            // Create user first
            createActiveUser(TEST_EMAIL);

            RegisterRequest request = new RegisterRequest(
                    TEST_EMAIL, TEST_PASSWORD, "Duplicate User", null
            );

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code").value("USER-001"))
                    .andExpect(jsonPath("$.title").value("Email Already Registered"));
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class Login {

        @Test
        @DisplayName("should login and return 200 with JWT")
        void shouldLogin_andReturnJwt() throws Exception {
            // Create user with known password
            JpaUser user = createActiveUser(TEST_EMAIL);

            LoginRequest request = new LoginRequest(TEST_EMAIL, TEST_PASSWORD);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken", notNullValue()))
                    .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                    .andExpect(header().exists("Set-Cookie"));
        }

        @Test
        @DisplayName("should return 401 when password wrong")
        void shouldReturn401_whenPasswordWrong() throws Exception {
            createActiveUser(TEST_EMAIL);

            LoginRequest request = new LoginRequest(TEST_EMAIL, "WrongPassword1!");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error_code").value("AUTH-401"));
        }

        @Test
        @DisplayName("should return 401 when user not found")
        void shouldReturn401_whenUserNotFound() throws Exception {
            LoginRequest request = new LoginRequest("nonexistent@example.com", TEST_PASSWORD);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.title").value("Invalid Credentials"));
        }
    }

    @Nested
    @DisplayName("GET /auth/me")
    class GetMe {

        @Test
        @DisplayName("should return user profile when authenticated")
        void shouldReturnProfile_whenAuthenticated() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = jwtService.generateAccessToken(user.getId(), TEST_EMAIL, null, null);

            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        @DisplayName("should return 401 when not authenticated")
        void shouldReturn401_whenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Cross-service JWT compatibility")
    class JwtCompatibility {

        @Test
        @DisplayName("token generated by user-service should be valid")
        void tokenGeneratedByUserService_shouldBeValid() throws Exception {
            JpaUser user = createActiveUser(TEST_EMAIL);
            String token = jwtService.generateAccessToken(user.getId(), TEST_EMAIL, null, "OWNER");

            // Token should work to access protected endpoint
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());

            // Token should be parseable
            assertThat(jwtService.isValid(token)).isTrue();
            assertThat(jwtService.extractUserId(token)).isEqualTo(user.getId());
        }
    }

    // ============ Helper Methods ============

    private JpaUser createActiveUser(String email) {
        String hashedPassword = passwordEncoder.encode(TEST_PASSWORD);
        JpaUser user = new JpaUser(
                UUID.randomUUID(), email, hashedPassword, "Test User", null,
                "ACTIVE", Instant.now(), Instant.now()
        );
        return userRepository.save(user);
    }
}
