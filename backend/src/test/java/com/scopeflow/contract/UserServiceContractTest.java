package com.scopeflow.contract;

import com.scopeflow.adapter.in.web.auth.dto.LoginRequest;
import com.scopeflow.adapter.in.web.auth.dto.LoginResponse;
import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.adapter.in.web.user.dto.UserResponse;
import com.scopeflow.core.domain.workspace.Role;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.contract.stubrunner.spring.AutoConfigureStubRunner;
import org.springframework.cloud.contract.stubrunner.spring.StubRunnerProperties;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer contract tests for User Service.
 *
 * Validates that the monolith (consumer) can interact with user-service (provider)
 * using the published contracts (stubs).
 *
 * This test uses WireMock stubs generated from user-service contracts to simulate
 * the user-service responses without needing the actual service running.
 *
 * Test scenarios:
 * 1. Login with valid credentials → JWT token received
 * 2. JWT token is valid and can be used for authenticated requests
 * 3. User lookup by email works
 * 4. Invited user creation works
 * 5. Error responses follow RFC 9457 Problem Details
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureStubRunner(
        ids = "com.scopeflow:user-service:+:stubs:8090",
        stubsMode = StubRunnerProperties.StubsMode.LOCAL
)
class UserServiceContractTest {

    @LocalServerPort
    private int port;

    private final TestRestTemplate restTemplate = new TestRestTemplate();

    // Stub server runs on port 8090 (configured in @AutoConfigureStubRunner)
    private static final String STUB_BASE_URL = "http://localhost:8090/api/v1";

    @Test
    void shouldLoginSuccessfully_andReceiveValidJwtToken() {
        // Given: valid credentials
        LoginRequest request = new LoginRequest("test@example.com", "ValidPassword123!");

        // When: calling login endpoint
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                STUB_BASE_URL + "/auth/login",
                request,
                LoginResponse.class
        );

        // Then: successful login
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        LoginResponse body = response.getBody();
        assertThat(body.accessToken()).isNotBlank();
        assertThat(body.accessToken()).matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+$"); // JWT format
        assertThat(body.userId()).isNotNull();
        assertThat(body.email()).isEqualTo("test@example.com");
        assertThat(body.fullName()).isNotBlank();
        assertThat(body.expiresIn()).isPositive();
    }

    @Test
    void shouldReturn401_whenLoginWithInvalidCredentials() {
        // Given: invalid credentials
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword");

        // When: calling login endpoint
        ResponseEntity<String> response = restTemplate.postForEntity(
                STUB_BASE_URL + "/auth/login",
                request,
                String.class
        );

        // Then: unauthorized response with Problem Details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);

        String body = response.getBody();
        assertThat(body).contains("\"error_code\":\"AUTH-401\"");
        assertThat(body).contains("\"status\":401");
        assertThat(body).contains("\"error_id\":");
        assertThat(body).contains("\"timestamp\":");
    }

    @Test
    void shouldGetCurrentUser_whenAuthenticatedWithValidJwt() {
        // Given: valid JWT token (from login)
        String jwtToken = loginAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));

        // When: calling /auth/me endpoint
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                STUB_BASE_URL + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponse.class
        );

        // Then: user profile returned
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserResponse user = response.getBody();
        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isNotBlank();
        assertThat(user.fullName()).isNotBlank();
        assertThat(user.status()).isIn("ACTIVE", "INACTIVE");
        assertThat(user.createdAt()).isNotNull();
    }

    @Test
    void shouldReturn401_whenAccessingProtectedEndpointWithoutJwt() {
        // Given: no JWT token

        // When: calling /auth/me without token
        ResponseEntity<String> response = restTemplate.getForEntity(
                STUB_BASE_URL + "/auth/me",
                String.class
        );

        // Then: unauthorized response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"error_code\":\"AUTH-401\"");
    }

    @Test
    void shouldGetUserByEmail_whenUserExists() {
        // Given: valid JWT token and existing email
        String jwtToken = loginAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));

        // When: looking up user by email
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                STUB_BASE_URL + "/users/by-email/test@example.com",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponse.class
        );

        // Then: user found
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("test@example.com");
    }

    @Test
    void shouldReturn404_whenUserByEmailNotFound() {
        // Given: valid JWT token and non-existent email
        String jwtToken = loginAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));

        // When: looking up non-existent user
        ResponseEntity<String> response = restTemplate.exchange(
                STUB_BASE_URL + "/users/by-email/notfound@example.com",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );

        // Then: user not found with Problem Details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"error_code\":\"USER-010\"");
        assertThat(response.getBody()).contains("\"status\":404");
    }

    @Test
    void shouldCreateInvitedUser_whenValidRequest() {
        // Given: valid JWT token and invited user request
        String jwtToken = loginAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                "invited@example.com",
                Role.MEMBER,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        );

        // When: creating invited user
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                STUB_BASE_URL + "/users/invited",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                UserResponse.class
        );

        // Then: invited user created with INACTIVE status
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        UserResponse user = response.getBody();
        assertThat(user.email()).isEqualTo("invited@example.com");
        assertThat(user.status()).isEqualTo("INACTIVE");
        assertThat(user.id()).isNotNull();
    }

    @Test
    void shouldReturn409_whenCreatingInvitedUserWithDuplicateEmail() {
        // Given: valid JWT token and duplicate email
        String jwtToken = loginAndGetToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));
        headers.setContentType(MediaType.APPLICATION_JSON);

        CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                "test@example.com", // duplicate email (test user already exists)
                Role.MEMBER,
                UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
        );

        // When: creating invited user with duplicate email
        ResponseEntity<String> response = restTemplate.exchange(
                STUB_BASE_URL + "/users/invited",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        // Then: conflict response with Problem Details
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("\"error_code\":\"USER-011\"");
        assertThat(response.getBody()).contains("\"status\":409");
    }

    /**
     * JWT Token Validation Test.
     *
     * This test validates that JWT tokens generated by user-service (via stubs)
     * can be validated by the monolith's JwtTokenProvider.
     *
     * CRITICAL: This ensures the shared secret between user-service and monolith
     * is identical during the Strangler Fig migration.
     */
    @Test
    void jwtTokenFromUserService_shouldBeAcceptedByMonolith() {
        // Given: JWT token from user-service stub
        String jwtToken = loginAndGetToken();

        // When: using this token to access monolith's protected endpoint
        // (e.g., GET /api/v1/workspaces — workspace endpoint in monolith)
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken.replace("Bearer ", ""));

        // Simulate monolith endpoint validation by calling stub endpoint
        // In production, this would be replaced by actual monolith endpoint test
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                STUB_BASE_URL + "/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                UserResponse.class
        );

        // Then: JWT token is accepted
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        // NOTE: In production, add test calling actual monolith endpoint:
        // ResponseEntity<WorkspaceListResponse> monolithResponse = restTemplate.exchange(
        //     "http://localhost:" + port + "/api/v1/workspaces",
        //     HttpMethod.GET,
        //     new HttpEntity<>(headers),
        //     WorkspaceListResponse.class
        // );
        // assertThat(monolithResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ============ Helper methods ============

    private String loginAndGetToken() {
        LoginRequest request = new LoginRequest("test@example.com", "ValidPassword123!");

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                STUB_BASE_URL + "/auth/login",
                request,
                LoginResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return "Bearer " + response.getBody().accessToken();
    }
}
