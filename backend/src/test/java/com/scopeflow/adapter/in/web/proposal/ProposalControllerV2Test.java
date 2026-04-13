package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.proposal.dto.CreateProposalRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProposalControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("ProposalControllerV2")
class ProposalControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProposalService proposalService;

    @Test
    @DisplayName("GET /proposals/{id} returns 404 when proposal not found")
    @WithScopeFlowUser
    void getById_shouldReturn404_whenNotFound() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        given(proposalService.findById(any(ProposalId.class)))
                .willThrow(new ProposalNotFoundException("Proposal not found: " + id));

        // When / Then
        mockMvc.perform(get("/proposals/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Proposal Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /proposals/{id}/publish returns 409 when not DRAFT")
    @WithScopeFlowUser
    void publish_shouldReturn409_whenInvalidState() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        given(proposalService.publish(any(ProposalId.class)))
                .willThrow(new InvalidProposalStateException(
                        "Can only publish DRAFT proposal, current status: PUBLISHED"
                ));

        // When / Then
        mockMvc.perform(post("/proposals/" + id + "/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    @Test
    @DisplayName("POST /proposals returns 400 when request body missing required fields")
    @WithScopeFlowUser
    void create_shouldReturn400_whenMissingRequiredFields() throws Exception {
        // Missing briefingId and proposalName
        String invalidBody = "{\"clientId\": \"" + UUID.randomUUID() + "\"}";

        mockMvc.perform(post("/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 404 when proposal not found (public)")
    void getApprovalPage_shouldReturn404_forPublicEndpoint() throws Exception {
        // Given - using ApprovalControllerV2 which is mapped to same /proposals
        UUID id = UUID.randomUUID();
        given(proposalService.findById(any(ProposalId.class)))
                .willThrow(new ProposalNotFoundException("Proposal not found: " + id));

        // When / Then - public endpoint, no auth needed
        mockMvc.perform(get("/proposals/" + id + "/approve"))
                .andExpect(status().isNotFound());
    }

    // ============ I2: Workspace isolation for versions endpoint ============

    @Test
    @DisplayName("GET /proposals/{id}/versions returns 403 when proposal belongs to another workspace")
    @WithScopeFlowUser
    void getVersions_shouldReturn403_whenProposalBelongsToAnotherWorkspace() throws Exception {
        // Given — proposal exists but belongs to a DIFFERENT workspace than the JWT
        UUID id = UUID.randomUUID();
        UUID differentWorkspaceId = UUID.randomUUID(); // not the authenticated workspace

        ProposalDraft proposalInDifferentWorkspace = new ProposalDraft(
                ProposalId.of(id),
                new WorkspaceId(differentWorkspaceId), // different workspace
                UUID.randomUUID(),
                new BriefingSessionId(UUID.randomUUID()),
                "Test Proposal",
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );

        given(proposalService.findById(ProposalId.of(id)))
                .willReturn(Optional.of(proposalInDifferentWorkspace));

        // When / Then — workspace mismatch → 403 Forbidden
        mockMvc.perform(get("/proposals/" + id + "/versions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /proposals/{id}/versions returns 404 when proposal not found")
    @WithScopeFlowUser
    void getVersions_shouldReturn404_whenProposalNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        given(proposalService.findById(any(ProposalId.class)))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/proposals/" + id + "/versions"))
                .andExpect(status().isNotFound());
    }

    // ============ I3: Pagination for proposals list ============

    @Test
    @DisplayName("GET /proposals returns paginated response with default page params")
    @WithScopeFlowUser
    void list_shouldReturnPagedResponse_withDefaults() throws Exception {
        // Given
        given(proposalService.findByWorkspace(any(WorkspaceId.class)))
                .willReturn(List.of());

        // When / Then — response should have pagination fields
        mockMvc.perform(get("/proposals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    @DisplayName("GET /proposals respects page and size parameters")
    @WithScopeFlowUser
    void list_shouldRespectPageAndSizeParams() throws Exception {
        // Given — 3 proposals total
        UUID wsId = UUID.randomUUID();
        List<Proposal> proposals = java.util.stream.IntStream.range(0, 3)
                .mapToObj(i -> (Proposal) new ProposalDraft(
                        ProposalId.of(UUID.randomUUID()),
                        new WorkspaceId(wsId),
                        UUID.randomUUID(),
                        new BriefingSessionId(UUID.randomUUID()),
                        "Proposal " + i, null,
                        java.time.Instant.now(), java.time.Instant.now()
                ))
                .toList();

        given(proposalService.findByWorkspace(any(WorkspaceId.class))).willReturn(proposals);

        // When / Then — page 0, size 2 → 2 items, totalPages = 2
        mockMvc.perform(get("/proposals?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @DisplayName("GET /proposals enforces max page size of 100")
    @WithScopeFlowUser
    void list_shouldEnforceMaxPageSize() throws Exception {
        // Given
        given(proposalService.findByWorkspace(any(WorkspaceId.class))).willReturn(List.of());

        // When / Then — size=500 should be capped to 100
        mockMvc.perform(get("/proposals?size=500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
