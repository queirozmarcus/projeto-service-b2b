package com.scopeflow.user.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.auth.dto.RegisterRequest;
import com.scopeflow.user.adapter.out.persistence.JpaUserSpringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E tests for user registration with invalid email validation.
 *
 * Validates complete flow:
 * Frontend → HTTP POST → AuthController → Application Service → Email VO validation
 * → InvalidValueObjectException → GlobalExceptionHandler → RFC 9457 Problem Details
 *
 * Sprint 8: End-to-End validation of VO-001 error flow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Register Invalid Email E2E Tests")
class RegisterInvalidEmailE2ETest {

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

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserSpringRepository userRepository;

    private static final String VALID_PASSWORD = "SecurePass123!";
    private static final String VALID_NAME = "Test User";

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("should return 400 with VO-001 when email format is invalid")
    void shouldReturn400WithVO001_whenEmailFormatInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "invalid-email", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"))
                .andExpect(jsonPath("$.title").value("Invalid Value Object"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.error_id", matchesPattern("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("should return 400 with VO-001 when email missing @ symbol")
    void shouldReturn400WithVO001_whenEmailMissingAtSymbol() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "testexample.com", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"));
    }

    @Test
    @DisplayName("should return 400 with VO-001 when email missing domain")
    void shouldReturn400WithVO001_whenEmailMissingDomain() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.title").value("Invalid Value Object"));
    }

    @Test
    @DisplayName("should return 400 with VO-001 when email is blank")
    void shouldReturn400WithVO001_whenEmailIsBlank() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"));
    }

    @Test
    @DisplayName("should return 400 with VO-001 when email contains spaces")
    void shouldReturn400WithVO001_whenEmailContainsSpaces() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test user@example.com", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.error_code").value("VO-001"));
    }

    @Test
    @DisplayName("should return different error code when password is invalid (not VO-001)")
    void shouldReturnDifferentErrorCode_whenPasswordInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "valid@example.com", "weak", VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/problem+json"))
                // Validar que NÃO é VO-001 (é erro de password, não de Email VO)
                .andExpect(jsonPath("$.error_code").value(not("VO-001")));
    }

    @Test
    @DisplayName("should successfully register when email is valid")
    void shouldRegisterSuccessfully_whenEmailIsValid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "valid@example.com", VALID_PASSWORD, VALID_NAME, null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.email").value("valid@example.com"));
    }
}
