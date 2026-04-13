package com.scopeflow.adapter.in.web.integration;

import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.proposal.JpaApprovalWorkflow;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ApprovalControllerV2.
 *
 * These endpoints are public (no JWT required) — client-facing approval flow.
 */
@DisplayName("Approval REST Integration")
class ApprovalIntegrationTest extends ScopeFlowIntegrationTestBase {

    // ============ GET /proposals/{id}/approve ============

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 200 when proposal is PUBLISHED")
    void getApprovalPage_shouldReturn200_whenPublished() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Set to PUBLISHED
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        // Public endpoint — no auth header
        mockMvc.perform(get("/proposals/" + proposal.getId() + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposal.getId().toString()))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 404 when proposal not found")
    void getApprovalPage_shouldReturn404_whenProposalNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        mockMvc.perform(get("/proposals/" + nonExistentId + "/approve"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Proposal Not Found"));
    }

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 409 when proposal is DRAFT")
    void getApprovalPage_shouldReturn409_whenProposalIsDraft() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());
        // Proposal is DRAFT — not available for approval

        mockMvc.perform(get("/proposals/" + proposal.getId() + "/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    // ============ POST /proposals/{id}/approve ============

    @Test
    @DisplayName("POST /proposals/{id}/approve records approval and returns 201")
    void approve_shouldReturn201_whenValidRequest() throws Exception {
        // Full flow: workspace → briefing → proposal (PUBLISHED) → workflow → approval
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Publish the proposal
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        // Create an approval workflow with one pending approver
        JpaApprovalWorkflow workflow = createApprovalWorkflow(proposal.getId(), "client@example.com");

        String approvalBody = """
            {
              "approverName": "John Client",
              "approverEmail": "client@example.com"
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody)
                        .header("X-Forwarded-For", "203.0.113.10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approverName").value("John Client"))
                .andExpect(jsonPath("$.approverEmail").value("client@example.com"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.ipAddress").value("203.0.113.10"));
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 400 when approverName missing")
    void approve_shouldReturn400_whenApproverNameMissing() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        String body = """
            {
              "approverEmail": "client@example.com"
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 409 when proposal is DRAFT, not PUBLISHED")
    void approve_shouldReturn409_whenProposalNotPublished() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());
        // Proposal stays DRAFT

        String body = """
            {
              "approverName": "Client Name",
              "approverEmail": "client@example.com"
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve transitions proposal to APPROVED when all approvers agree")
    void approve_shouldTransitionProposalToApproved_whenAllApproved() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Publish
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        // Create workflow with single approver
        createApprovalWorkflow(proposal.getId(), "sole-approver@example.com");

        String body = """
            {
              "approverName": "Sole Approver",
              "approverEmail": "sole-approver@example.com"
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Verify proposal transitioned to APPROVED in DB
        JpaProposal approved = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo("APPROVED");
    }

    // ============ Private helpers ============

    /**
     * Create an approval workflow with one PENDING approver.
     */
    private JpaApprovalWorkflow createApprovalWorkflow(UUID proposalId, String approverEmail) {
        UUID workflowId = UUID.randomUUID();

        JpaApprovalWorkflow workflow = new JpaApprovalWorkflow(
                workflowId,
                proposalId,
                "IN_PROGRESS",
                Instant.now(),
                null
        );
        workflowRepository.save(workflow);

        com.scopeflow.adapter.out.persistence.proposal.JpaApproval approval =
                new com.scopeflow.adapter.out.persistence.proposal.JpaApproval(
                        UUID.randomUUID(),
                        workflowId,
                        null,
                        approverEmail,
                        "PENDING",
                        null,
                        null,
                        null
                );
        approvalRepository.save(approval);

        return workflow;
    }
}
