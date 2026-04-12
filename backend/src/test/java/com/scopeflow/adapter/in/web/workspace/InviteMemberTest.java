package com.scopeflow.adapter.in.web.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.workspace.dto.InviteMemberRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.workspace.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

import static com.scopeflow.core.domain.workspace.Role.MEMBER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for inviteMember in WorkspaceControllerV2.
 *
 * Tests: invite new user (creates via user-service), invite existing user (lookup by email),
 * role enforcement, validation, duplicate member handling.
 */
@WebMvcTest(WorkspaceControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("WorkspaceControllerV2 — inviteMember")
class InviteMemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @MockBean
    private RestTemplate authServiceRestTemplate;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID EXISTING_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final String INVITE_URL = "/workspaces/" + WORKSPACE_ID + "/members/invite";

    // ============ Invite new user (not yet registered) ============

    @Nested
    @DisplayName("Invite new user (not yet registered)")
    class InviteNewUserTests {

        @Test
        @DisplayName("POST invite returns 201 when user does not exist — creates via user-service")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenInvitingNewUser() throws Exception {
            // GET /users/by-email → 404
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), "{}".getBytes(), null));

            // POST /users/invited → 201 with new user id
            String createdBody = """
                    {"id":"%s","email":"newuser@example.com","status":"INACTIVE","fullName":"Newuser"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), eq(String.class)
            )).willReturn(ResponseEntity.status(HttpStatus.CREATED).body(createdBody));

            InviteMemberRequest request = new InviteMemberRequest("newuser@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("MEMBER"))
                    .andExpect(jsonPath("$.status").value("INVITED"));
        }

        @Test
        @DisplayName("POST invite calls workspaceService.inviteMember with userId from user-service response")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldCallInviteMember_withCorrectUserId() throws Exception {
            // GET /users/by-email → 404
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willThrow(HttpClientErrorException.create(
                    HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(), "{}".getBytes(), null));

            // POST /users/invited → 201
            String createdBody = """
                    {"id":"%s","email":"fresh@example.com","status":"INACTIVE","fullName":"Fresh"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), eq(String.class)
            )).willReturn(ResponseEntity.status(HttpStatus.CREATED).body(createdBody));

            InviteMemberRequest request = new InviteMemberRequest("fresh@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            then(workspaceService).should().inviteMember(
                    eq(new WorkspaceId(WORKSPACE_ID)),
                    argThat(uid -> uid.value().equals(EXISTING_USER_ID)),
                    eq(MEMBER)
            );
        }
    }

    // ============ Invite existing user ============

    @Nested
    @DisplayName("Invite existing user")
    class InviteExistingUserTests {

        @Test
        @DisplayName("POST invite returns 201 when user already exists in user-service")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenUserAlreadyExists() throws Exception {
            // GET /users/by-email → 200 with existing user
            String existingBody = """
                    {"id":"%s","email":"existing@example.com","status":"ACTIVE","fullName":"Existing"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willReturn(ResponseEntity.ok(existingBody));

            InviteMemberRequest request = new InviteMemberRequest("existing@example.com", Role.ADMIN);

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.role").value("ADMIN"));
        }

        @Test
        @DisplayName("POST invite does NOT call POST /users/invited when user already exists")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldNotCreateUser_whenUserExists() throws Exception {
            String existingBody = """
                    {"id":"%s","email":"existing@example.com","status":"ACTIVE","fullName":"Existing"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willReturn(ResponseEntity.ok(existingBody));

            InviteMemberRequest request = new InviteMemberRequest("existing@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // POST /users/invited must NEVER be called when user already exists
            then(authServiceRestTemplate).should(never()).exchange(
                    contains("/users/invited"), eq(HttpMethod.POST), any(), eq(String.class)
            );
        }
    }

    // ============ Role enforcement ============

    @Nested
    @DisplayName("Role enforcement")
    class RoleEnforcementTests {

        @Test
        @DisplayName("POST invite returns 403 when caller is MEMBER (not OWNER/ADMIN)")
        @WithScopeFlowUser(role = "MEMBER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn403_whenCallerIsMember() throws Exception {
            InviteMemberRequest request = new InviteMemberRequest("someone@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(authServiceRestTemplate);
            verifyNoInteractions(workspaceService);
        }

        @Test
        @DisplayName("POST invite returns 201 when caller is ADMIN (allowed to invite)")
        @WithScopeFlowUser(role = "ADMIN", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn201_whenCallerIsAdmin() throws Exception {
            String existingBody = """
                    {"id":"%s","email":"newteam@example.com","status":"ACTIVE","fullName":"Team"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willReturn(ResponseEntity.ok(existingBody));

            InviteMemberRequest request = new InviteMemberRequest("newteam@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    // ============ Validation ============

    @Nested
    @DisplayName("Validation")
    class ValidationTests {

        @Test
        @DisplayName("POST invite returns 400 when email is invalid format (Bean Validation, no proxy)")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn400_whenEmailIsInvalidFormat() throws Exception {
            String invalidBody = "{\"email\": \"not-an-email\", \"role\": \"MEMBER\"}";

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type")
                            .value("https://api.scopeflow.com/errors/validation-error"));

            verifyNoInteractions(authServiceRestTemplate);
        }

        @Test
        @DisplayName("POST invite returns 400 when email is missing (Bean Validation, no proxy)")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn400_whenEmailIsMissing() throws Exception {
            String invalidBody = "{\"role\": \"MEMBER\"}";

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authServiceRestTemplate);
        }
    }

    // ============ Duplicate member ============

    @Nested
    @DisplayName("Duplicate member")
    class DuplicateMemberTests {

        @Test
        @DisplayName("POST invite returns 409 when member already belongs to workspace")
        @WithScopeFlowUser(role = "OWNER", workspaceId = "00000000-0000-0000-0000-000000000002")
        void shouldReturn409_whenMemberAlreadyInWorkspace() throws Exception {
            String existingBody = """
                    {"id":"%s","email":"duplicate@example.com","status":"ACTIVE","fullName":"Dup"}
                    """.formatted(EXISTING_USER_ID);
            given(authServiceRestTemplate.exchange(
                    contains("/users/by-email/"), eq(HttpMethod.GET), any(), eq(String.class)
            )).willReturn(ResponseEntity.ok(existingBody));

            doThrow(new MemberAlreadyExistsException("User already a member of this workspace"))
                    .when(workspaceService).inviteMember(any(), any(), any());

            InviteMemberRequest request = new InviteMemberRequest("duplicate@example.com", MEMBER);

            mockMvc.perform(post(INVITE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }
}
