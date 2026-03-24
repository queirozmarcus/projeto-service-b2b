package com.scopeflow.adapter.in.web.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.workspace.dto.InviteMemberRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.user.*;
import com.scopeflow.core.domain.workspace.*;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for C2: inviteMember real logic in WorkspaceControllerV2.
 *
 * Tests: invite new user (creates UserInactive), invite existing user (adds to workspace),
 * role enforcement, validation, duplicate member handling.
 */
@WebMvcTest(WorkspaceControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("WorkspaceControllerV2 — inviteMember (C2)")
class InviteMemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final String INVITE_URL = "/workspaces/" + WORKSPACE_ID + "/members/invite";

    // ============ C2: invite new user (not yet registered) ============

    @Nested
    @DisplayName("Invite new user (not yet registered)")
    class InviteNewUserTests {

        @Test
        @DisplayName("POST invite returns 201 when inviting new user — creates UserInactive")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenInvitingNewUser() throws Exception {
            // Given — user does NOT exist
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.empty());
            given(passwordEncoder.encode(any())).willReturn("$2a$hashed");
            // saveInvitedUser is void — no stub needed
            // inviteMember is void — no stub needed

            InviteMemberRequest request = new InviteMemberRequest("newuser@example.com", Role.MEMBER);

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("MEMBER"))
                    .andExpect(jsonPath("$.status").value("INVITED"));
        }

        @Test
        @DisplayName("POST invite calls saveInvitedUser when email is not registered")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldCallSaveInvitedUser_whenEmailNotRegistered() throws Exception {
            // Given
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.empty());
            given(passwordEncoder.encode(any())).willReturn("$2a$hashed");

            InviteMemberRequest request = new InviteMemberRequest("fresh@example.com", Role.MEMBER);

            mockMvc.perform(post(INVITE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then — saveInvitedUser was called (new user was created)
            then(userService).should().saveInvitedUser(any(UserInactive.class));
        }
    }

    @Nested
    @DisplayName("Invite existing user")
    class InviteExistingUserTests {

        @Test
        @DisplayName("POST invite returns 201 when user already exists — skips user creation")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenUserAlreadyExists() throws Exception {
            // Given — user already registered
            UUID existingUserId = UUID.randomUUID();
            UserActive existingUser = new UserActive(
                    new UserId(existingUserId),
                    new Email("existing@example.com"),
                    new PasswordHash("$2a$hash"),
                    "Existing User", null,
                    Instant.now(), Instant.now()
            );
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.of(existingUser));

            InviteMemberRequest request = new InviteMemberRequest("existing@example.com", Role.ADMIN);

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("POST invite does NOT call saveInvitedUser when user already exists")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldNotCallSaveInvitedUser_whenUserExists() throws Exception {
            // Given — user exists
            UUID existingUserId = UUID.randomUUID();
            UserActive existingUser = new UserActive(
                    new UserId(existingUserId),
                    new Email("existing@example.com"),
                    new PasswordHash("$2a$hash"),
                    "Existing User", null,
                    Instant.now(), Instant.now()
            );
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.of(existingUser));

            InviteMemberRequest request = new InviteMemberRequest("existing@example.com", Role.MEMBER);

            mockMvc.perform(post(INVITE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Then — saveInvitedUser was NOT called (existing user, no new user created)
            then(userService).should(never()).saveInvitedUser(any());
        }
    }

    @Nested
    @DisplayName("Role enforcement (C2)")
    class RoleEnforcementTests {

        @Test
        @DisplayName("POST invite returns 403 when caller is MEMBER (not OWNER/ADMIN)")
        @WithScopeFlowUser(role = "MEMBER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn403_whenCallerIsMember() throws Exception {
            // Given — MEMBER role cannot invite
            InviteMemberRequest request = new InviteMemberRequest("someone@example.com", Role.MEMBER);

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            // No user lookup or workspace mutation should happen
            verifyNoInteractions(userService);
            verifyNoInteractions(workspaceService);
        }

        @Test
        @DisplayName("POST invite returns 201 when caller is ADMIN (allowed to invite)")
        @WithScopeFlowUser(role = "ADMIN", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenCallerIsAdmin() throws Exception {
            // Given
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.empty());
            given(passwordEncoder.encode(any())).willReturn("$2a$hashed");

            InviteMemberRequest request = new InviteMemberRequest("newteam@example.com", Role.MEMBER);

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("Validation (C2)")
    class ValidationTests {

        @Test
        @DisplayName("POST invite returns 400 when email is invalid format")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn400_whenEmailIsInvalidFormat() throws Exception {
            // Given — invalid email
            String invalidBody = "{\"email\": \"not-an-email\", \"role\": \"MEMBER\"}";

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.scopeflow.com/errors/validation-error"));
        }

        @Test
        @DisplayName("POST invite returns 400 when email is missing")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn400_whenEmailIsMissing() throws Exception {
            // Given
            String invalidBody = "{\"role\": \"MEMBER\"}";

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Duplicate member (C2)")
    class DuplicateMemberTests {

        @Test
        @DisplayName("POST invite returns 409 when member already belongs to workspace")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn409_whenMemberAlreadyInWorkspace() throws Exception {
            // Given — user exists
            UUID existingUserId = UUID.randomUUID();
            UserActive existingUser = new UserActive(
                    new UserId(existingUserId),
                    new Email("duplicate@example.com"),
                    new PasswordHash("$2a$hash"),
                    "Dup User", null,
                    Instant.now(), Instant.now()
            );
            given(userService.getUserByEmail(any(Email.class))).willReturn(Optional.of(existingUser));
            doThrow(new MemberAlreadyExistsException("User already a member of this workspace"))
                    .when(workspaceService).inviteMember(any(), any(), any());

            InviteMemberRequest request = new InviteMemberRequest("duplicate@example.com", Role.MEMBER);

            // When / Then
            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
}
