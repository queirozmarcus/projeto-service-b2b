package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for I2 (workspace isolation on versions) and I3 (pagination on proposals).
 *
 * I2: GET /proposals/{id}/versions must enforce workspace ownership — 403 if different workspace.
 * I3: GET /proposals must respect page/size params and enforce max size of 100.
 */
@WebMvcTest({ProposalControllerV2.class, ApprovalControllerV2.class})
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("ProposalControllerV2 — workspace isolation (I2) and pagination (I3)")
class WorkspaceIsolationAndPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProposalService proposalService;

    // Fixed UUIDs from @WithScopeFlowUser defaults
    private static final UUID AUTH_WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // ============ I2: Workspace isolation on versions ============

    @Nested
    @DisplayName("I2 — GET /proposals/{id}/versions workspace isolation")
    class VersionsWorkspaceIsolationTests {

        @Test
        @DisplayName("403 when proposal belongs to a different workspace")
        @WithScopeFlowUser
        void shouldReturn403_whenProposalBelongsToDifferentWorkspace() throws Exception {
            // Given — proposal in workspace OTHER than authenticated workspace
            UUID proposalId = UUID.randomUUID();
            UUID otherWorkspaceId = UUID.randomUUID(); // not AUTH_WORKSPACE_ID

            ProposalDraft proposalInOtherWorkspace = new ProposalDraft(
                    ProposalId.of(proposalId),
                    new WorkspaceId(otherWorkspaceId),
                    UUID.randomUUID(),
                    new BriefingSessionId(UUID.randomUUID()),
                    "Other WS Proposal", null,
                    Instant.now(), Instant.now()
            );
            given(proposalService.findById(ProposalId.of(proposalId)))
                    .willReturn(Optional.of(proposalInOtherWorkspace));

            // When / Then — workspace B cannot see workspace A's versions
            mockMvc.perform(get("/proposals/" + proposalId + "/versions"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("200 when proposal belongs to authenticated workspace")
        @WithScopeFlowUser
        void shouldReturn200_whenProposalBelongsToAuthWorkspace() throws Exception {
            // Given — proposal in the SAME workspace as authenticated user
            UUID proposalId = UUID.randomUUID();

            ProposalDraft proposal = new ProposalDraft(
                    ProposalId.of(proposalId),
                    new WorkspaceId(AUTH_WORKSPACE_ID), // same workspace
                    UUID.randomUUID(),
                    new BriefingSessionId(UUID.randomUUID()),
                    "Same WS Proposal", null,
                    Instant.now(), Instant.now()
            );
            given(proposalService.findById(ProposalId.of(proposalId)))
                    .willReturn(Optional.of(proposal));
            given(proposalService.findVersions(any(ProposalId.class)))
                    .willReturn(List.of());

            // When / Then
            mockMvc.perform(get("/proposals/" + proposalId + "/versions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("404 when proposal does not exist (version endpoint)")
        @WithScopeFlowUser
        void shouldReturn404_whenProposalNotFound() throws Exception {
            // Given
            UUID proposalId = UUID.randomUUID();
            given(proposalService.findById(any(ProposalId.class))).willReturn(Optional.empty());

            // When / Then
            mockMvc.perform(get("/proposals/" + proposalId + "/versions"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 on GET /proposals/{id} when proposal belongs to different workspace")
        @WithScopeFlowUser
        void getById_shouldReturn403_whenWrongWorkspace() throws Exception {
            // Given
            UUID proposalId = UUID.randomUUID();
            UUID differentWorkspaceId = UUID.randomUUID();

            ProposalDraft proposalInOtherWs = new ProposalDraft(
                    ProposalId.of(proposalId),
                    new WorkspaceId(differentWorkspaceId),
                    UUID.randomUUID(),
                    new BriefingSessionId(UUID.randomUUID()),
                    "Forbidden Proposal", null,
                    Instant.now(), Instant.now()
            );
            given(proposalService.findById(ProposalId.of(proposalId)))
                    .willReturn(Optional.of(proposalInOtherWs));

            // When / Then
            mockMvc.perform(get("/proposals/" + proposalId))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ I3: Pagination ============

    @Nested
    @DisplayName("I3 — GET /proposals pagination")
    class PaginationTests {

        @Test
        @DisplayName("Default page=0, size=20 when no params given")
        @WithScopeFlowUser
        void shouldUseDefaultPageAndSize_whenNoParamsGiven() throws Exception {
            // Given
            given(proposalService.findByWorkspace(any(WorkspaceId.class)))
                    .willReturn(List.of());

            // When / Then
            mockMvc.perform(get("/proposals"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(20))
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.first").value(true))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("Custom page=1 and size=5 respected in response")
        @WithScopeFlowUser
        void shouldRespectCustomPageAndSize() throws Exception {
            // Given — 12 proposals total, page=1, size=5 → returns items 6-10
            List<Proposal> proposals = buildProposals(12);
            given(proposalService.findByWorkspace(any(WorkspaceId.class))).willReturn(proposals);

            // When / Then
            mockMvc.perform(get("/proposals?page=1&size=5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(5))
                    .andExpect(jsonPath("$.totalElements").value(12))
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.first").value(false))
                    .andExpect(jsonPath("$.last").value(false));
        }

        @Test
        @DisplayName("Max size enforced at 100 — size=500 is capped to 100")
        @WithScopeFlowUser
        void shouldCapSizeAt100() throws Exception {
            // Given
            given(proposalService.findByWorkspace(any(WorkspaceId.class)))
                    .willReturn(List.of());

            // When / Then
            mockMvc.perform(get("/proposals?size=500"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100));
        }

        @Test
        @DisplayName("Max size enforced at 100 — size=1000 is capped to 100")
        @WithScopeFlowUser
        void shouldCapSizeAt100_forVeryLargeSize() throws Exception {
            given(proposalService.findByWorkspace(any(WorkspaceId.class)))
                    .willReturn(List.of());

            mockMvc.perform(get("/proposals?size=1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size").value(100));
        }

        @Test
        @DisplayName("Last page flag is true when on the last page")
        @WithScopeFlowUser
        void shouldSetLastFlag_onFinalPage() throws Exception {
            // Given — 3 proposals, page=0, size=3 → last=true
            List<Proposal> proposals = buildProposals(3);
            given(proposalService.findByWorkspace(any(WorkspaceId.class))).willReturn(proposals);

            mockMvc.perform(get("/proposals?page=0&size=3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.last").value(true))
                    .andExpect(jsonPath("$.content.length()").value(3));
        }

        @Test
        @DisplayName("Page beyond total returns empty content")
        @WithScopeFlowUser
        void shouldReturnEmpty_whenPageBeyondTotal() throws Exception {
            // Given — 2 proposals, page=5
            List<Proposal> proposals = buildProposals(2);
            given(proposalService.findByWorkspace(any(WorkspaceId.class))).willReturn(proposals);

            mockMvc.perform(get("/proposals?page=5&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Status filter delegates to findByWorkspaceAndStatus")
        @WithScopeFlowUser
        void shouldDelegateToStatusFilter_whenStatusParamGiven() throws Exception {
            // Given
            given(proposalService.findByWorkspaceAndStatus(any(WorkspaceId.class), eq(ProposalStatus.DRAFT)))
                    .willReturn(List.of());

            mockMvc.perform(get("/proposals?status=DRAFT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    // ============ Helpers ============

    private List<Proposal> buildProposals(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> (Proposal) new ProposalDraft(
                        ProposalId.of(UUID.randomUUID()),
                        new WorkspaceId(AUTH_WORKSPACE_ID),
                        UUID.randomUUID(),
                        new BriefingSessionId(UUID.randomUUID()),
                        "Proposal " + i, null,
                        Instant.now(), Instant.now()
                ))
                .toList();
    }
}
