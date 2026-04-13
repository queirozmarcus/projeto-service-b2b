package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.proposal.dto.UpdateProposalRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for the PUT and DELETE endpoints added in Sprint 6.
 *
 * Existing CRUD tests (GET, POST, list) remain in ProposalControllerV2Test.
 * This class focuses exclusively on the new rename and soft-delete operations.
 */
@WebMvcTest(ProposalControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("ProposalControllerV2 — rename and soft-delete")
class ProposalCrudControllerTest {

    // Fixed UUID from @WithScopeFlowUser default (workspaceId)
    private static final UUID TEST_WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProposalService proposalService;

    // ============ PUT /proposals/{id} ============

    @Nested
    @DisplayName("PUT /proposals/{id}")
    class UpdateProposal {

        @Test
        @DisplayName("should return 200 with updated proposal when name is valid")
        @WithScopeFlowUser
        void shouldReturn200_whenNameIsValid() throws Exception {
            // Given
            UUID id = UUID.randomUUID();
            ProposalDraft renamed = new ProposalDraft(
                    ProposalId.of(id),
                    new WorkspaceId(TEST_WORKSPACE_ID),
                    UUID.randomUUID(),
                    new BriefingSessionId(UUID.randomUUID()),
                    "New Name",
                    null,
                    Instant.now(),
                    Instant.now()
            );
            given(proposalService.renameProposal(
                    eq(ProposalId.of(id)),
                    any(WorkspaceId.class),
                    eq("New Name")
            )).willReturn(renamed);

            // When / Then
            mockMvc.perform(put("/proposals/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateProposalRequest("New Name"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.proposalName").value("New Name"))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("should return 400 when proposalName is blank")
        @WithScopeFlowUser
        void shouldReturn400_whenNameIsBlank() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(put("/proposals/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"proposalName\": \"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 when request body is missing")
        @WithScopeFlowUser
        void shouldReturn400_whenBodyIsMissing() throws Exception {
            UUID id = UUID.randomUUID();

            mockMvc.perform(put("/proposals/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 404 when proposal not found")
        @WithScopeFlowUser
        void shouldReturn404_whenProposalNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            given(proposalService.renameProposal(any(), any(), any()))
                    .willThrow(new ProposalNotFoundException("Proposal not found: " + id));

            mockMvc.perform(put("/proposals/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateProposalRequest("New Name"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should return 409 when proposal is not DRAFT")
        @WithScopeFlowUser
        void shouldReturn409_whenProposalIsNotDraft() throws Exception {
            UUID id = UUID.randomUUID();
            given(proposalService.renameProposal(any(), any(), any()))
                    .willThrow(new InvalidProposalStateException(
                            "Can only rename a DRAFT proposal, current status: PUBLISHED"
                    ));

            mockMvc.perform(put("/proposals/" + id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateProposalRequest("New Name"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
        }

    }

    // ============ DELETE /proposals/{id} ============

    @Nested
    @DisplayName("DELETE /proposals/{id}")
    class DeleteProposal {

        @Test
        @DisplayName("should return 204 when proposal soft-deleted successfully")
        @WithScopeFlowUser
        void shouldReturn204_whenSoftDeleteSucceeds() throws Exception {
            UUID id = UUID.randomUUID();
            // service.deleteProposal does not throw — successful deletion

            mockMvc.perform(delete("/proposals/" + id))
                    .andExpect(status().isNoContent());

            verify(proposalService).deleteProposal(
                    eq(ProposalId.of(id)),
                    any(WorkspaceId.class)
            );
        }

        @Test
        @DisplayName("should return 404 when proposal not found")
        @WithScopeFlowUser
        void shouldReturn404_whenProposalNotFound() throws Exception {
            UUID id = UUID.randomUUID();
            willThrow(new ProposalNotFoundException("Proposal not found: " + id))
                    .given(proposalService).deleteProposal(any(), any());

            mockMvc.perform(delete("/proposals/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

    }

    // ============ Adapter: soft delete ============

    @Nested
    @DisplayName("Soft delete transparency")
    class SoftDeleteTransparency {

        @Test
        @DisplayName("GET /proposals/{id} should return 404 after soft-delete (via service returning empty)")
        @WithScopeFlowUser
        void getById_shouldReturn404_afterSoftDelete() throws Exception {
            // Simulates: proposal was soft-deleted, @SQLRestriction causes findById to return empty
            UUID id = UUID.randomUUID();
            given(proposalService.findById(ProposalId.of(id))).willReturn(Optional.empty());

            mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/proposals/" + id))
                    .andExpect(status().isNotFound());
        }
    }
}
