package com.scopeflow.adapter.in.web.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthControllerV2.
 *
 * Full Spring Boot context + Testcontainers PostgreSQL + real Flyway migrations.
 * Tests verify the entire auth slice: HTTP → Controller → UserService → JPA → DB.
 */
@DisplayName("Auth REST Integration")
class AuthIntegrationTest extends ScopeFlowIntegrationTestBase {

    // ============ POST /auth/register ============

    @Test
    @DisplayName("POST /auth/register creates user and returns tokens")
    void register_shouldReturn201_withTokens() throws Exception {
        String body = """
            {
              "email": "newuser@example.com",
              "password": "Password1!",
              "fullName": "New User",
              "phone": "+5511999999999"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/register returns 409 when email already registered")
    void register_shouldReturn409_whenEmailAlreadyTaken() throws Exception {
        // Pre-create user with same email
        createActiveUser(java.util.UUID.randomUUID(), "existing@example.com");

        String body = """
            {
              "email": "existing@example.com",
              "password": "Password1!",
              "fullName": "Another User"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email Already Registered"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when password too weak")
    void register_shouldReturn400_whenPasswordTooWeak() throws Exception {
        String body = """
            {
              "email": "user@example.com",
              "password": "weak",
              "fullName": "Test User"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/validation-error"));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when email missing")
    void register_shouldReturn400_whenEmailMissing() throws Exception {
        String body = """
            {
              "password": "Password1!",
              "fullName": "Test User"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ============ POST /auth/login ============

    @Test
    @DisplayName("POST /auth/login returns 200 with tokens on valid credentials")
    void login_shouldReturn200_withTokens() throws Exception {
        // Register user first via API to ensure password is properly hashed
        String registerBody = """
            {
              "email": "login@example.com",
              "password": "Password1!",
              "fullName": "Login User"
            }
            """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
            {
              "email": "login@example.com",
              "password": "Password1!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 when password is wrong")
    void login_shouldReturn401_whenPasswordWrong() throws Exception {
        // Register first
        String registerBody = """
            {
              "email": "wrongpass@example.com",
              "password": "Password1!",
              "fullName": "User"
            }
            """;
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
            {
              "email": "wrongpass@example.com",
              "password": "WrongPass999!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Credentials"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 when email not found")
    void login_shouldReturn401_whenEmailNotFound() throws Exception {
        String loginBody = """
            {
              "email": "notfound@example.com",
              "password": "Password1!"
            }
            """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized());
    }

    // ============ GET /auth/me ============

    @Test
    @DisplayName("GET /auth/me returns 401 when no token provided")
    void me_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /auth/me returns 200 with user info when authenticated")
    void me_shouldReturn200_whenAuthenticated() throws Exception {
        // Register and get token
        String registerBody = """
            {
              "email": "metest@example.com",
              "password": "Password1!",
              "fullName": "Me User"
            }
            """;

        var result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();

        String responseJson = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).get("accessToken").asText();

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("metest@example.com"));
    }
}
