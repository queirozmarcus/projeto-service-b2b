package com.scopeflow.adapter.in.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.adapter.out.userservice.AuthProxyAdapter;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.workspace.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController — proxy behavior.
 *
 * Validates that the controller correctly proxies requests to user-service
 * and forwards status codes and response bodies.
 */
@WebMvcTest(UserController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("UserController")
class UserControllerTest {

    private static final UUID TEST_WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthProxyAdapter authProxyAdapter;

    // ============ GET /api/v1/users/by-email/{email} ============

    @Nested
    @DisplayName("GET /api/v1/users/by-email/{email}")
    class GetByEmail {

        @Test
        @DisplayName("should proxy to user-service and return 200 when user exists")
        @WithScopeFlowUser
        void shouldReturn200_whenEmailExists() throws Exception {
            String upstreamBody = """
                    {"id":"00000000-0000-0000-0000-000000000001","email":"existing@example.com",
                     "fullName":"Existing User","status":"ACTIVE","createdAt":"2024-01-01T00:00:00Z"}
                    """;

            given(authProxyAdapter.proxy(
                    contains("/users/by-email/"), eq(HttpMethod.GET), isNull(), any(HttpHeaders.class)
            )).willReturn(ResponseEntity.ok(upstreamBody));

            mockMvc.perform(get("/api/v1/users/by-email/{email}", "existing@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(content().json(upstreamBody));
        }

        @Test
        @DisplayName("should forward 404 from user-service when user not found")
        @WithScopeFlowUser
        void shouldForward404_whenEmailDoesNotExist() throws Exception {
            String problemJson = """
                    {"type":"https://api.scopeflow.com/errors/user-not-found",
                     "title":"User Not Found","status":404,"error_code":"USER-010"}
                    """;

            // AuthProxyAdapter.proxy() catches HttpClientErrorException and returns ResponseEntity
            given(authProxyAdapter.proxy(
                    contains("/users/by-email/"), eq(HttpMethod.GET), isNull(), any(HttpHeaders.class)
            )).willReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemJson));

            mockMvc.perform(get("/api/v1/users/by-email/{email}", "nonexistent@example.com"))
                    .andExpect(status().isNotFound());
        }
    }

    // ============ POST /api/v1/users/invited ============

    @Nested
    @DisplayName("POST /api/v1/users/invited")
    class CreateInvited {

        @Test
        @DisplayName("should proxy to user-service and return 201 when valid request")
        @WithScopeFlowUser
        void shouldReturn201_whenValidRequest() throws Exception {
            UUID invitedByUserId = UUID.randomUUID();
            String email = "invited@example.com";

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    email,
                    Role.MEMBER,
                    invitedByUserId
            );

            String upstreamBody = """
                    {"id":"00000000-0000-0000-0000-000000000002","email":"invited@example.com",
                     "fullName":"Invited User","status":"INACTIVE","createdAt":"2024-01-01T00:00:00Z"}
                    """;

            given(authProxyAdapter.proxy(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
            )).willReturn(ResponseEntity.status(HttpStatus.CREATED).body(upstreamBody));

            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().json(upstreamBody));
        }

        @Test
        @DisplayName("should forward 409 from user-service when email already exists")
        @WithScopeFlowUser
        void shouldForward409_whenEmailExists() throws Exception {
            UUID invitedByUserId = UUID.randomUUID();

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "existing@example.com",
                    Role.MEMBER,
                    invitedByUserId
            );

            String problemJson = """
                    {"type":"https://api.scopeflow.com/errors/duplicate-email",
                     "title":"Duplicate Email","status":409,"error_code":"USER-011"}
                    """;

            given(authProxyAdapter.proxy(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), any(HttpHeaders.class)
            )).willReturn(ResponseEntity.status(HttpStatus.CONFLICT).body(problemJson));

            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 400 from Bean Validation when email is blank (no proxy call)")
        @WithScopeFlowUser
        void shouldReturn400_whenEmailIsBlank() throws Exception {
            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "",
                    Role.MEMBER,
                    UUID.randomUUID()
            );

            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("VALIDATION-400")))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }
    }
}
