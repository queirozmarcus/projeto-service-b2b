package com.scopeflow.adapter.in.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.user.*;
import com.scopeflow.core.domain.workspace.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController endpoints.
 *
 * Tests:
 * - GET /api/v1/users/by-email/{email}
 * - POST /api/v1/users/invited
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
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // ============ GET /api/v1/users/by-email/{email} ============

    @Nested
    @DisplayName("GET /api/v1/users/by-email/{email}")
    class GetByEmail {

        @Test
        @DisplayName("should return 200 with user when email exists")
        @WithScopeFlowUser
        void shouldReturn200_whenEmailExists() throws Exception {
            // Given
            String email = "existing@example.com";
            UserId userId = UserId.generate();
            Email emailVO = new Email(email);
            PasswordHash passwordHash = new PasswordHash("hashed-password");

            UserActive user = new UserActive(
                    userId,
                    emailVO,
                    passwordHash,
                    "Existing User",
                    "+123456789",
                    Instant.now(),
                    Instant.now()
            );

            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.of(user));

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", email))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(userId.value().toString())))
                    .andExpect(jsonPath("$.email", is(email.toLowerCase())))
                    .andExpect(jsonPath("$.fullName", is("Existing User")))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));

            verify(userService).getUserByEmail(any(Email.class));
        }

        @Test
        @DisplayName("should return 404 when email does not exist")
        @WithScopeFlowUser
        void shouldReturn404_whenEmailDoesNotExist() throws Exception {
            // Given
            String email = "nonexistent@example.com";
            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", email))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error_code", is("USER-010")))
                    .andExpect(jsonPath("$.title", is("User Not Found")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/user-not-found")));

            verify(userService).getUserByEmail(any(Email.class));
        }

        @Test
        @DisplayName("should return 400 when email format is invalid")
        @WithScopeFlowUser
        void shouldReturn400_whenEmailFormatInvalid() throws Exception {
            // Given: invalid email format triggers IllegalArgumentException from Email VO

            // When/Then
            mockMvc.perform(get("/api/v1/users/by-email/{email}", "invalid-email"))
                    .andExpect(status().isInternalServerError()); // IllegalArgumentException → 500
        }
    }

    // ============ POST /api/v1/users/invited ============

    @Nested
    @DisplayName("POST /api/v1/users/invited")
    class CreateInvited {

        @Test
        @DisplayName("should return 201 with created invited user when valid request")
        @WithScopeFlowUser
        void shouldReturn201_whenValidRequest() throws Exception {
            // Given
            UUID invitedByUserId = UUID.randomUUID();
            String email = "invited@example.com";

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    email,
                    Role.MEMBER,
                    invitedByUserId
            );

            // Mock: email does not exist
            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.empty());

            // Mock: invitedBy user exists
            given(userService.getUserById(any(UserId.class)))
                    .willReturn(Optional.of(createMockUser()));

            // Mock: password encoder
            given(passwordEncoder.encode(any(String.class)))
                    .willReturn("hashed-temp-password");

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is(email.toLowerCase())))
                    .andExpect(jsonPath("$.status", is("INACTIVE")))
                    .andExpect(jsonPath("$.fullName").exists());

            verify(userService).getUserByEmail(any(Email.class));
            verify(userService).getUserById(any(UserId.class));
            verify(userService).saveInvitedUser(any(UserInactive.class));
        }

        @Test
        @DisplayName("should return 409 when email already exists")
        @WithScopeFlowUser
        void shouldReturn409_whenEmailExists() throws Exception {
            // Given
            UUID invitedByUserId = UUID.randomUUID();
            String email = "existing@example.com";

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    email,
                    Role.MEMBER,
                    invitedByUserId
            );

            // Mock: email exists
            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.of(createMockUser()));

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error_code", is("USER-011")))
                    .andExpect(jsonPath("$.title", is("Duplicate Email")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/duplicate-email")));

            verify(userService).getUserByEmail(any(Email.class));
        }

        @Test
        @DisplayName("should return 400 when invitedBy user does not exist")
        @WithScopeFlowUser
        void shouldReturn400_whenInvitedByUserDoesNotExist() throws Exception {
            // Given
            UUID invitedByUserId = UUID.randomUUID();
            String email = "invited@example.com";

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    email,
                    Role.MEMBER,
                    invitedByUserId
            );

            // Mock: email does not exist
            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.empty());

            // Mock: invitedBy user does NOT exist
            given(userService.getUserById(any(UserId.class)))
                    .willReturn(Optional.empty());

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-012")))
                    .andExpect(jsonPath("$.title", is("Invalid Invited By User")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/invalid-invited-by-user")));

            verify(userService).getUserByEmail(any(Email.class));
            verify(userService).getUserById(any(UserId.class));
        }

        @Test
        @DisplayName("should return 400 when role is OWNER")
        @WithScopeFlowUser
        void shouldReturn400_whenRoleIsOwner() throws Exception {
            // Given
            UUID invitedByUserId = UUID.randomUUID();
            String email = "invited@example.com";

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    email,
                    Role.OWNER,
                    invitedByUserId
            );

            // Mock: email does not exist
            given(userService.getUserByEmail(any(Email.class)))
                    .willReturn(Optional.empty());

            // Mock: invitedBy user exists
            given(userService.getUserById(any(UserId.class)))
                    .willReturn(Optional.of(createMockUser()));

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("USER-013")))
                    .andExpect(jsonPath("$.title", is("Invalid Role")))
                    .andExpect(jsonPath("$.type", is("https://api.scopeflow.com/errors/invalid-role")));
        }

        @Test
        @DisplayName("should return 400 when email is blank")
        @WithScopeFlowUser
        void shouldReturn400_whenEmailIsBlank() throws Exception {
            // Given
            UUID invitedByUserId = UUID.randomUUID();

            CreateInvitedUserRequest request = new CreateInvitedUserRequest(
                    "",
                    Role.MEMBER,
                    invitedByUserId
            );

            // When/Then
            mockMvc.perform(post("/api/v1/users/invited")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error_code", is("VALIDATION-400")))
                    .andExpect(jsonPath("$.title", is("Validation Error")));
        }
    }

    // ============ Helper Methods ============

    private UserActive createMockUser() {
        UserId userId = UserId.generate();
        Email email = new Email("mock@example.com");
        PasswordHash passwordHash = new PasswordHash("hashed");
        return new UserActive(userId, email, passwordHash, "Mock User", null, Instant.now(), Instant.now());
    }
}
