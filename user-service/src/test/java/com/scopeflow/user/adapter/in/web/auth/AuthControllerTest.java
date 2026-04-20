package com.scopeflow.user.adapter.in.web.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.user.adapter.in.web.auth.dto.LoginRequest;
import com.scopeflow.user.adapter.in.web.auth.dto.RegisterRequest;
import com.scopeflow.user.application.usecase.AuthenticateUserUseCase;
import com.scopeflow.user.application.usecase.RefreshTokenUseCase;
import com.scopeflow.user.application.usecase.RegisterUserUseCase;
import com.scopeflow.user.config.JwtAuthenticationEntryPoint;
import com.scopeflow.user.config.JwtAuthenticationFilter;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.config.TestSecurityConfig;
import com.scopeflow.user.domain.exception.EmailAlreadyRegisteredException;
import com.scopeflow.user.domain.exception.InvalidCredentialsException;
import com.scopeflow.user.domain.model.*;
import com.scopeflow.user.domain.port.out.TokenIssuer;
import com.scopeflow.user.domain.port.out.UserBlocklist;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockBean
    private RefreshTokenUseCase refreshTokenUseCase;

    @MockBean
    private TokenIssuer tokenIssuer;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserBlocklist userBlocklist;

    private static final String BCRYPT_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    @Test
    @DisplayName("POST /auth/register returns 201 with token response")
    void register_shouldReturn201_whenValidRequest() throws Exception {
        UserActive mockUser = new UserActive(
                UserId.generate(), new Email("user@example.com"), new PasswordHash(BCRYPT_HASH),
                "Test User", "+5511999999999", Instant.now(), Instant.now()
        );
        given(registerUserUseCase.execute(any(), any(), any(), any())).willReturn(mockUser);
        given(tokenIssuer.issueAccessToken(any(), any(), any())).willReturn("access-token");
        given(tokenIssuer.issueRefreshToken(any())).willReturn("refresh-token");
        given(tokenIssuer.accessTokenExpirationSeconds()).willReturn(900L);
        given(tokenIssuer.refreshTokenExpirationSeconds()).willReturn(604800L);

        RegisterRequest request = new RegisterRequest(
                "user@example.com", "Password1!", "Test User", "+5511999999999"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.email").value("user@example.com"));
    }

    @Test
    @DisplayName("POST /auth/register returns 400 when password too weak")
    void register_shouldReturn400_whenWeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest("user@example.com", "weak", "Test User", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/validation-error"));
    }

    @Test
    @DisplayName("POST /auth/register returns 409 when email already registered")
    void register_shouldReturn409_whenEmailTaken() throws Exception {
        given(registerUserUseCase.execute(any(), any(), any(), any()))
                .willThrow(new EmailAlreadyRegisteredException("Email already registered: user@example.com"));

        RegisterRequest request = new RegisterRequest("user@example.com", "Password1!", "Test User", null);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email Already Registered"));
    }

    @Test
    @DisplayName("POST /auth/login returns 401 when credentials invalid")
    void login_shouldReturn401_whenInvalidCredentials() throws Exception {
        given(authenticateUserUseCase.execute(any(), any()))
                .willThrow(new InvalidCredentialsException("Invalid email or password"));

        LoginRequest request = new LoginRequest("user@example.com", "WrongPass1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Invalid Credentials"));
    }

    @Test
    @DisplayName("POST /auth/login returns 200 with tokens on success")
    void login_shouldReturn200_whenValidCredentials() throws Exception {
        UserActive mockUser = new UserActive(
                UserId.generate(), new Email("user@example.com"), new PasswordHash(BCRYPT_HASH),
                "Test User", null, Instant.now(), Instant.now()
        );
        var authTokens = new AuthenticateUserUseCase.AuthTokens("access-token", "refresh-token");
        given(authenticateUserUseCase.execute(any(), any()))
                .willReturn(new AuthenticateUserUseCase.Result(mockUser, authTokens));
        given(tokenIssuer.issueAccessToken(any(), any(), any())).willReturn("access-token");
        given(tokenIssuer.issueRefreshToken(any())).willReturn("refresh-token");
        given(tokenIssuer.accessTokenExpirationSeconds()).willReturn(900L);
        given(tokenIssuer.refreshTokenExpirationSeconds()).willReturn(604800L);

        LoginRequest request = new LoginRequest("user@example.com", "Password1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }
}
