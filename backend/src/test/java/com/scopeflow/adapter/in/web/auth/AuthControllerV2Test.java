package com.scopeflow.adapter.in.web.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.auth.dto.LoginRequest;
import com.scopeflow.adapter.in.web.auth.dto.RegisterRequest;
import com.scopeflow.adapter.out.userservice.AuthProxyAdapter;
import com.scopeflow.config.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("AuthControllerV2")
class AuthControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthProxyAdapter authProxyAdapter;

    // ============ register ============

    @Test
    @DisplayName("POST /auth/register proxies to user-service and returns 201")
    void register_shouldProxy_andReturn201() throws Exception {
        String upstreamBody = """
                {"accessToken":"tok","expiresIn":900,"userId":"00000000-0000-0000-0000-000000000001",
                 "email":"user@example.com","fullName":"Test User"}
                """;
        HttpHeaders upstreamHeaders = new HttpHeaders();
        upstreamHeaders.add(HttpHeaders.SET_COOKIE, "refreshToken=rt; HttpOnly; Path=/");

        given(authProxyAdapter.proxy(
                contains("/auth/register"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
        )).willReturn(ResponseEntity.status(HttpStatus.CREATED).headers(upstreamHeaders).body(upstreamBody));

        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Password1!", "Test User", "+5511999999999"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=")))
                .andExpect(content().json(upstreamBody));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when request validation fails (no proxy call)")
    void register_shouldReturn400_whenWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "weak", "Test User", null
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/validation-error"));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when email invalid (no proxy call)")
    void register_shouldReturn400_whenInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "not-an-email", "Password1!", "Test User", null
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register forwards 409 from user-service")
    void register_shouldForward409_whenUserServiceReturnsConflict() throws Exception {
        String problemJson = """
                {"type":"https://api.scopeflow.com/errors/email-already-registered",
                 "title":"Email Already Registered","status":409}
                """;

        // AuthProxyAdapter.proxy() catches HttpClientErrorException internally and returns ResponseEntity
        given(authProxyAdapter.proxy(
                contains("/auth/register"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
        )).willReturn(ResponseEntity.status(HttpStatus.CONFLICT).body(problemJson));

        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Password1!", "Test User", null
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ============ login ============

    @Test
    @DisplayName("POST /auth/login proxies to user-service and returns 200")
    void login_shouldProxy_andReturn200() throws Exception {
        String upstreamBody = """
                {"accessToken":"tok","expiresIn":900,"userId":"00000000-0000-0000-0000-000000000001",
                 "email":"user@example.com","fullName":"Test User"}
                """;
        HttpHeaders upstreamHeaders = new HttpHeaders();
        upstreamHeaders.add(HttpHeaders.SET_COOKIE, "refreshToken=rt; HttpOnly; Path=/");

        given(authProxyAdapter.proxy(
                contains("/auth/login"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
        )).willReturn(ResponseEntity.ok().headers(upstreamHeaders).body(upstreamBody));

        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("HttpOnly")))
                .andExpect(content().json(upstreamBody));
    }

    @Test
    @DisplayName("POST /auth/login forwards 401 from user-service")
    void login_shouldForward401_whenInvalidCredentials() throws Exception {
        String problemJson = """
                {"type":"https://api.scopeflow.com/errors/invalid-credentials",
                 "title":"Invalid Credentials","status":401}
                """;

        given(authProxyAdapter.proxy(
                contains("/auth/login"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
        )).willReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemJson));

        LoginRequest request = new LoginRequest("user@example.com", "WrongPass1!");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ============ refresh ============

    @Test
    @DisplayName("POST /auth/refresh proxies cookies to user-service")
    void refresh_shouldProxyCookies() throws Exception {
        String upstreamBody = """
                {"accessToken":"new-tok","expiresIn":900}
                """;

        given(authProxyAdapter.proxy(
                contains("/auth/refresh"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
        )).willReturn(ResponseEntity.ok(upstreamBody));

        mockMvc.perform(post("/auth/refresh")
                        .header("Cookie", "refreshToken=valid-rt"))
                .andExpect(status().isOk())
                .andExpect(content().json(upstreamBody));
    }
}
