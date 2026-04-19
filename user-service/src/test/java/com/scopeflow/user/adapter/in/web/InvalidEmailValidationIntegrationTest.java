package com.scopeflow.user.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.auth.dto.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Email validation at HTTP layer.
 *
 * <p>Validates that {@link com.scopeflow.user.domain.model.Email} validation failures
 * are properly mapped to RFC 9457 Problem Details responses by {@link GlobalExceptionHandler}.
 *
 * <p>Tests the full stack: HTTP request → DTO binding → Email VO construction → Exception handling → RFC 9457 response.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Email Validation - HTTP Integration")
class InvalidEmailValidationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when email format is invalid")
    void shouldReturn400_whenEmailFormatInvalid() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "invalid-email",      // Missing @ and domain
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"))
                .andExpect(jsonPath("$.title").value("Invalid Value Object"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("Invalid email format")))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.vo_type").value("Email"))
                .andExpect(jsonPath("$.error_id").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when email is missing @ symbol")
    void shouldReturn400_whenEmailMissingAtSymbol() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "userexample.com",
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"))
                .andExpect(jsonPath("$.title").value("Invalid Value Object"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("Invalid email format")))
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.vo_type").value("Email"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when email is missing domain")
    void shouldReturn400_whenEmailMissingDomain() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "user@",
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"))
                .andExpect(jsonPath("$.title").value("Invalid Value Object"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.vo_type").value("Email"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when email has invalid characters")
    void shouldReturn400_whenEmailHasInvalidCharacters() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "user name@example.com",  // Space is invalid
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/invalid-value-object"))
                .andExpect(jsonPath("$.vo_type").value("Email"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 when email is empty")
    void shouldReturn400_whenEmailIsEmpty() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "",
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/validation-error"))
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.error_code").value("VALIDATION-400"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should validate RFC 9457 structure completely")
    void shouldValidateRfc9457Structure() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
                "bad-email",
                "Password1!",
                "Test User",
                null
        );

        // When/Then
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // RFC 9457 mandatory fields
                .andExpect(jsonPath("$.type").isString())
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").isString())
                // Custom extension fields
                .andExpect(jsonPath("$.error_code").value("VO-001"))
                .andExpect(jsonPath("$.vo_type").value("Email"))
                .andExpect(jsonPath("$.error_id").isString())
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
