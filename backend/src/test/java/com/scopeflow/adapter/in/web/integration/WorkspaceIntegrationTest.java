package com.scopeflow.adapter.in.web.integration;

import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WorkspaceControllerV2.
 *
 * Covers workspace CRUD and member management with real PostgreSQL + JWT authentication.
 */
@DisplayName("Workspace REST Integration")
class WorkspaceIntegrationTest extends ScopeFlowIntegrationTestBase {

    // ============ POST /workspaces ============

    @Test
    @DisplayName("POST /workspaces creates workspace and returns 201")
    void create_shouldReturn201_withWorkspaceResponse() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        String body = """
            {
              "name": "My New Agency",
              "niche": "design"
            }
            """;

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My New Agency"))
                .andExpect(jsonPath("$.niche").value("design"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /workspaces returns 409 when workspace name already exists")
    void create_shouldReturn409_whenNameAlreadyExists() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        // Pre-create workspace with same name
        createWorkspace(auth.userId(), "Existing Agency");

        String body = """
            {
              "name": "Existing Agency",
              "niche": "social-media"
            }
            """;

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Workspace Name Already Exists"));
    }

    @Test
    @DisplayName("POST /workspaces returns 400 when name missing")
    void create_shouldReturn400_whenNameMissing() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        String body = """
            {
              "niche": "social-media"
            }
            """;

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.scopeflow.com/errors/validation-error"));
    }

    @Test
    @DisplayName("POST /workspaces returns 401 when unauthenticated")
    void create_shouldReturn401_whenUnauthenticated() throws Exception {
        String body = """
            {
              "name": "Agency",
              "niche": "social-media"
            }
            """;

        mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ============ GET /workspaces/{id} ============

    @Test
    @DisplayName("GET /workspaces/{id} returns 200 with workspace details")
    void getById_shouldReturn200_whenFound() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        mockMvc.perform(get("/workspaces/" + auth.workspaceId())
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(auth.workspaceId().toString()))
                .andExpect(jsonPath("$.name").value(TEST_WORKSPACE_NAME));
    }

    @Test
    @DisplayName("GET /workspaces/{id} returns 404 when workspace not found")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/workspaces/" + nonExistentId)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Workspace Not Found"));
    }

    // ============ PUT /workspaces/{id} ============

    @Test
    @DisplayName("PUT /workspaces/{id} updates workspace name and returns 200")
    void update_shouldReturn200_withUpdatedWorkspace() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        String body = """
            {
              "name": "Updated Agency Name",
              "niche": "landing-page"
            }
            """;

        mockMvc.perform(put("/workspaces/" + auth.workspaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Agency Name"));
    }

    // ============ GET /workspaces/{id}/members ============

    @Test
    @DisplayName("GET /workspaces/{id}/members returns member list")
    void getMembers_shouldReturn200_withMemberList() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        mockMvc.perform(get("/workspaces/" + auth.workspaceId() + "/members")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    // ============ DELETE /workspaces/{id}/members/{memberId} ============

    @Test
    @DisplayName("DELETE /workspaces/{id}/members/{memberId} returns 409 when removing last owner")
    void removeMember_shouldReturn409_whenRemovingLastOwner() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        // Attempt to remove the only owner (self)
        var members = memberRepository.findByWorkspaceId(auth.workspaceId());
        assertThat(members).hasSize(1);
        UUID ownerId = members.get(0).getId();

        mockMvc.perform(delete("/workspaces/" + auth.workspaceId() + "/members/" + ownerId)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cannot Remove Last Owner"));
    }

    @Test
    @DisplayName("DELETE /workspaces/{id} deletes workspace and returns 204")
    void deleteWorkspace_shouldReturn204() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        mockMvc.perform(delete("/workspaces/" + auth.workspaceId())
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isNoContent());

        // Verify soft-delete: workspace still exists in DB with DELETED status
        JpaWorkspace workspace = workspaceRepository.findById(auth.workspaceId()).orElseThrow();
        assertThat(workspace.getStatus()).isEqualTo("DELETED");
    }
}
