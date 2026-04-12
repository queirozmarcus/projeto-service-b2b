package com.scopeflow.adapter.in.web.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthControllerV2.
 *
 * Full Spring Boot context + Testcontainers PostgreSQL + real Flyway migrations.
 * Auth requests are proxied to user-service; RestTemplate is mocked to simulate
 * user-service responses without requiring the service to be running.
 *
 * Tests verify: HTTP routing, header forwarding (Set-Cookie), Bean Validation,
 * security filter (JWT auth for protected endpoints).
 */
@DisplayName("Auth REST Integration")
class AuthIntegrationTest extends ScopeFlowIntegrationTestBase {

    @MockBean
    private RestTemplate authServiceRestTemplate;

    // ============ POST /auth/register ============

    @Test
    @DisplayName("POST /auth/register proxies to user-service and returns 201 with tokens")
    void register_shouldReturn201_withTokens() throws Exception {
        String upstreamBody = """
            {"accessToken":"test-access-tok","expiresIn":900,
             "userId":"00000000-0000-0000-0000-000000000001",
             "email":"newuser@example.com","fullName":"New User"}
            """;
        HttpHeaders upstreamHeaders = new HttpHeaders();
        upstreamHeaders.add(HttpHeaders.SET_COOKIE, "refreshToken=rt; HttpOnly; Path=/; SameSite=Lax");

        given(authServiceRestTemplate.exchange(
                contains("/auth/register"), eq(HttpMethod.POST), any(), eq(String.class)
        )).willReturn(ResponseEntity.status(HttpStatus.CREATED).headers(upstreamHeaders).body(upstreamBody));

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
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.email").value("newuser@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register forwards 409 from user-service when email already registered")
    void register_shouldReturn409_whenEmailAlreadyTaken() throws Exception {
        String problemJson = """
            {"type":"https://api.scopeflow.com/errors/email-already-registered",
             "title":"Email Already Registered","status":409,"error_code":"USER-001"}
            """;

        given(authServiceRestTemplate.exchange(
                contains("/auth/register"), eq(HttpMethod.POST), any(), eq(String.class)
        )).willThrow(HttpClientErrorException.create(
                HttpStatus.CONFLICT, "Conflict", new HttpHeaders(), problemJson.getBytes(), null));

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
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /auth/register returns 400 from Bean Validation when password too weak (no proxy)")
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
    @DisplayName("POST /auth/register returns 400 from Bean Validation when email missing (no proxy)")
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
    @DisplayName("POST /auth/login proxies to user-service and returns 200 with tokens")
    void login_shouldReturn200_withTokens() throws Exception {
        String upstreamBody = """
            {"accessToken":"login-tok","expiresIn":900,
             "userId":"00000000-0000-0000-0000-000000000001",
             "email":"login@example.com","fullName":"Login User"}
            """;
        HttpHeaders upstreamHeaders = new HttpHeaders();
        upstreamHeaders.add(HttpHeaders.SET_COOKIE, "refreshToken=rt; HttpOnly; Path=/; SameSite=Lax");

        given(authServiceRestTemplate.exchange(
                contains("/auth/login"), eq(HttpMethod.POST), any(), eq(String.class)
        )).willReturn(ResponseEntity.ok().headers(upstreamHeaders).body(upstreamBody));

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
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("POST /auth/login forwards 401 from user-service when credentials invalid")
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {
        String problemJson = """
            {"type":"https://api.scopeflow.com/errors/invalid-credentials",
             "title":"Invalid Credentials","status":401,"error_code":"AUTH-401"}
            """;

        given(authServiceRestTemplate.exchange(
                contains("/auth/login"), eq(HttpMethod.POST), any(), eq(String.class)
        )).willThrow(HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", new HttpHeaders(), problemJson.getBytes(), null));

        String loginBody = """
            {
              "email": "wrongpass@example.com",
              "password": "WrongPass999!"
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
    @DisplayName("GET /auth/me proxies with Authorization header and returns user profile")
    void me_shouldReturn200_whenAuthenticated() throws Exception {
        // Create a real user + workspace to generate a valid JWT (auth filter validates locally)
        var auth = setupAuthenticatedUser();

        String upstreamBody = """
            {"id":"%s","email":"%s","fullName":"Test User","status":"ACTIVE",
             "createdAt":"2024-01-01T00:00:00Z"}
            """.formatted(auth.userId(), TEST_USER_EMAIL);

        given(authServiceRestTemplate.exchange(
                contains("/auth/me"), eq(HttpMethod.GET), any(), eq(String.class)
        )).willReturn(ResponseEntity.ok(upstreamBody));

        mockMvc.perform(get("/auth/me")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TEST_USER_EMAIL));
    }
}
