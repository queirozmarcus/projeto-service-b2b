package com.scopeflow.adapter.in.web.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.workspace.dto.CreateWorkspaceRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.core.domain.workspace.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkspaceControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("WorkspaceControllerV2")
class WorkspaceControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("POST /workspaces returns 400 when name missing")
    @WithMockUser
    void create_shouldReturn400_whenNameMissing() throws Exception {
        String invalidBody = "{\"niche\": \"social-media\"}";

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type")
                        .value("https://api.scopeflow.com/errors/validation-error"));
    }

    @Test
    @DisplayName("POST /workspaces returns 409 when name already exists")
    @WithMockUser
    void create_shouldReturn409_whenNameAlreadyExists() throws Exception {
        // Given
        given(workspaceService.createWorkspace(any(), any(), any(), any()))
                .willThrow(new WorkspaceNameAlreadyExistsException("Workspace name already exists: MyAgency"));

        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "MyAgency", "social-media", null
        );

        // When / Then
        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Workspace Name Already Exists"));
    }

    @Test
    @DisplayName("GET /workspaces/{id} returns 404 when workspace not found")
    @WithMockUser
    void getById_shouldReturn404_whenNotFound() throws Exception {
        // Given
        given(workspaceService.getWorkspaceById(any()))
                .willThrow(new WorkspaceNotFoundException("Workspace not found"));

        // When / Then
        mockMvc.perform(get("/workspaces/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workspace Not Found"));
    }

    @Test
    @DisplayName("DELETE /workspaces/{id}/members/{memberId} returns 409 when removing last owner")
    @WithMockUser(roles = "OWNER")
    void removeMember_shouldReturn409_whenLastOwner() throws Exception {
        // Given
        given(workspaceService.removeMember(any(), any()))
                .willThrow(new CannotRemoveLastOwnerException("Cannot remove the only OWNER"));

        // When / Then
        mockMvc.perform(delete("/workspaces/00000000-0000-0000-0000-000000000001/members/00000000-0000-0000-0000-000000000002"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cannot Remove Last Owner"));
    }
}
