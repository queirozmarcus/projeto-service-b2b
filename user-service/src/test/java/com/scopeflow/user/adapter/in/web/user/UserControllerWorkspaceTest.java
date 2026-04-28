package com.scopeflow.user.adapter.in.web.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.user.adapter.in.web.user.dto.UpdateUserWorkspaceRequest;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.application.usecase.BlockUserByIdUseCase;
import com.scopeflow.user.application.usecase.InviteUserUseCase;
import com.scopeflow.user.application.usecase.UpdateUserWorkspaceUseCase;
import com.scopeflow.user.config.InternalTokenProperties;
import com.scopeflow.user.config.JwtAuthenticationEntryPoint;
import com.scopeflow.user.config.JwtAuthenticationFilter;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.config.ScopeFlowPrincipal;
import com.scopeflow.user.config.TestSecurityConfig;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.exception.UserStateException;
import com.scopeflow.user.domain.exception.WorkspaceAlreadyAssignedException;
import com.scopeflow.user.domain.model.UserId;
import com.scopeflow.user.domain.port.out.UserBlocklist;
import com.scopeflow.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("UserController — PATCH /{userId}/workspace")
class UserControllerWorkspaceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private InviteUserUseCase inviteUserUseCase;

    @MockBean
    private BlockUserByIdUseCase blockUserByIdUseCase;

    @MockBean
    private UpdateUserWorkspaceUseCase updateUserWorkspaceUseCase;

    @MockBean
    private InternalTokenProperties internalTokenProperties;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserBlocklist userBlocklist;

    private static final String INTERNAL_TOKEN = "dev-internal-token-change-in-prod";

    @BeforeEach
    void setUp() {
        given(internalTokenProperties.getInternalToken()).willReturn(INTERNAL_TOKEN);
        SecurityContextHolder.clearContext();
    }

    // ============ Helper ============

    private void authenticateAs(UUID userId) {
        ScopeFlowPrincipal principal = new ScopeFlowPrincipal(userId, "user@example.com", null, "OWNER");
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String workspaceBody(UUID workspaceId) throws Exception {
        return objectMapper.writeValueAsString(new UpdateUserWorkspaceRequest(workspaceId));
    }

    // ============ Tests ============

    @Test
    @DisplayName("should return 204 when owner calls with own JWT")
    void shouldReturn204_whenOwnerCallsWithOwnJwt() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        authenticateAs(userId);
        given(updateUserWorkspaceUseCase.execute(any(UserId.class), eq(workspaceId)))
                .willReturn(null);

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 204 when valid X-Internal-Token is provided")
    void shouldReturn204_whenInternalTokenProvided() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        // No JWT principal — simulates monolith internal call
        given(updateUserWorkspaceUseCase.execute(any(UserId.class), eq(workspaceId)))
                .willReturn(null);

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 403 when JWT sub does not match userId in path")
    void shouldReturn403_whenDifferentUserJwt() throws Exception {
        // Given
        UUID pathUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID(); // authenticated user != path userId
        UUID workspaceId = UUID.randomUUID();
        authenticateAs(differentUserId);

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", pathUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 403 when X-Internal-Token is invalid")
    void shouldReturn403_whenInvalidInternalToken() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        // No principal, wrong token

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 409 when workspace already assigned")
    void shouldReturn409_whenWorkspaceAlreadyAssigned() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        authenticateAs(userId);
        willThrow(new WorkspaceAlreadyAssignedException(new UserId(userId), workspaceId))
                .given(updateUserWorkspaceUseCase).execute(any(UserId.class), eq(workspaceId));

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error_code").value("USER-020"));
    }

    @Test
    @DisplayName("should return 422 when user state does not allow workspace assignment")
    void shouldReturn422_whenUserStateInvalid() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        authenticateAs(userId);
        willThrow(new UserStateException(new UserId(userId), "BLOCKED", "assign-workspace"))
                .given(updateUserWorkspaceUseCase).execute(any(UserId.class), eq(workspaceId));

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error_code").value("USER-021"));
    }

    @Test
    @DisplayName("should return 404 when user not found")
    void shouldReturn404_whenUserNotFound() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        authenticateAs(userId);
        willThrow(new UserNotFoundException(new UserId(userId)))
                .given(updateUserWorkspaceUseCase).execute(any(UserId.class), eq(workspaceId));

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workspaceBody(workspaceId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error_code").value("USER-010"));
    }
}
