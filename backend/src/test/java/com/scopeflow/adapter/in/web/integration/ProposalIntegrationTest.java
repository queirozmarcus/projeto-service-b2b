package com.scopeflow.adapter.in.web.integration;

import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProposalControllerV2.
 *
 * Covers proposal lifecycle: create, update scope, publish, list, get by ID.
 * All tests require authentication. Approval flow is covered in ApprovalIntegrationTest.
 */
@DisplayName("Proposal REST Integration")
class ProposalIntegrationTest extends ScopeFlowIntegrationTestBase {

    // ============ POST /proposals ============

    @Test
    @DisplayName("POST /proposals creates draft proposal and returns 201")
    void create_shouldReturn201_withDraftProposal() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());

        String body = String.format("""
            {
              "briefingId": "%s",
              "clientId": "%s",
              "proposalName": "Social Media Package Q2"
            }
            """, briefing.getId(), java.util.UUID.randomUUID());

        mockMvc.perform(post("/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalName").value("Social Media Package Q2"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    @DisplayName("POST /proposals returns 400 when briefingId missing")
    void create_shouldReturn400_whenBriefingIdMissing() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        String body = """
            {
              "clientId": "00000000-0000-0000-0000-000000000001",
              "proposalName": "Some Proposal"
            }
            """;

        mockMvc.perform(post("/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /proposals returns 401 when unauthenticated")
    void create_shouldReturn401_whenUnauthenticated() throws Exception {
        String body = """
            {
              "briefingId": "00000000-0000-0000-0000-000000000001",
              "clientId": "00000000-0000-0000-0000-000000000002",
              "proposalName": "Unauthenticated Proposal"
            }
            """;

        mockMvc.perform(post("/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ============ GET /proposals/{id} ============

    @Test
    @DisplayName("GET /proposals/{id} returns 200 with proposal details")
    void getById_shouldReturn200_whenProposalBelongsToWorkspace() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        mockMvc.perform(get("/proposals/" + proposal.getId())
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposal.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    @DisplayName("GET /proposals/{id} returns 404 when proposal not found")
    void getById_shouldReturn404_whenNotFound() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        String nonExistentId = java.util.UUID.randomUUID().toString();

        mockMvc.perform(get("/proposals/" + nonExistentId)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Proposal Not Found"));
    }

    @Test
    @DisplayName("GET /proposals/{id} returns 403 when proposal belongs to different workspace")
    void getById_shouldReturn403_whenWrongWorkspace() throws Exception {
        // Create proposal in workspace A
        AuthContext authA = setupAuthenticatedUser();
        JpaBriefingSession briefingA = createBriefingSession(authA.workspaceId());
        JpaProposal proposalA = createDraftProposal(authA.workspaceId(), briefingA.getId());

        // User B tries to access workspace A's proposal
        var userB = createActiveUser(java.util.UUID.randomUUID(), "userb@example.com");
        var workspaceB = createWorkspace(userB.getId(), "Agency B");
        addOwnerMember(workspaceB.getId(), userB.getId());
        String tokenB = bearerToken(userB.getId(), userB.getEmail(), workspaceB.getId(), "OWNER");

        mockMvc.perform(get("/proposals/" + proposalA.getId())
                        .header("Authorization", "Bearer " + tokenB.replace("Bearer ", "")))
                .andExpect(status().isForbidden());
    }

    // ============ GET /proposals ============

    @Test
    @DisplayName("GET /proposals returns list of proposals for current workspace")
    void list_shouldReturn200_withWorkspaceProposals() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        createDraftProposal(auth.workspaceId(), briefing.getId());
        createDraftProposal(auth.workspaceId(), briefing.getId());

        mockMvc.perform(get("/proposals")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("GET /proposals returns empty list when no proposals exist")
    void list_shouldReturn200_withEmptyList() throws Exception {
        AuthContext auth = setupAuthenticatedUser();

        mockMvc.perform(get("/proposals")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ============ PUT /proposals/{id}/scope ============

    @Test
    @DisplayName("PUT /proposals/{id}/scope updates scope and returns 200")
    void updateScope_shouldReturn200_withUpdatedProposal() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        String scopeBody = """
            {
              "deliverables": ["Instagram feed (12 posts/month)", "Stories (30/month)"],
              "exclusions": ["Paid advertising", "Video production"],
              "assumptions": ["Client provides brand assets"],
              "price": {
                "amount": 2500.00,
                "currency": "BRL",
                "model": "MONTHLY"
              },
              "timeline": {
                "estimatedDays": 30,
                "description": "30-day rolling month"
              }
            }
            """;

        mockMvc.perform(put("/proposals/" + proposal.getId() + "/scope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scopeBody)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.id").value(proposal.getId().toString()));
    }

    @Test
    @DisplayName("PUT /proposals/{id}/scope returns 409 when proposal is not DRAFT")
    void updateScope_shouldReturn409_whenNotDraft() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Manually set status to PUBLISHED
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        String scopeBody = """
            {
              "deliverables": ["Some deliverable"],
              "exclusions": [],
              "assumptions": [],
              "price": {"amount": 1000.00, "currency": "BRL", "model": "MONTHLY"},
              "timeline": {"estimatedDays": 15, "description": "Two weeks"}
            }
            """;

        mockMvc.perform(put("/proposals/" + proposal.getId() + "/scope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scopeBody)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    // ============ POST /proposals/{id}/publish ============

    @Test
    @DisplayName("POST /proposals/{id}/publish publishes a draft and returns 200")
    void publish_shouldReturn200_withPublishedProposal() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/publish")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Verify DB state
        JpaProposal updated = proposalRepository.findById(proposal.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    @DisplayName("POST /proposals/{id}/publish returns 409 when already published")
    void publish_shouldReturn409_whenAlreadyPublished() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Set to PUBLISHED directly
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/publish")
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    // ============ POST /proposals/{id}/approval ============

    @Test
    @DisplayName("POST /proposals/{id}/approval initiates approval workflow and returns 201")
    void initiateApproval_shouldReturn201_withWorkflow() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());

        // Publish first
        JpaProposal saved = proposalRepository.findById(proposal.getId()).orElseThrow();
        saved.setStatus("PUBLISHED");
        proposalRepository.save(saved);

        String approvalBody = """
            {
              "approverEmails": ["client@example.com", "sponsor@example.com"]
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalId").value(proposal.getId().toString()))
                .andExpect(jsonPath("$.approvals").isArray())
                .andExpect(jsonPath("$.approvals.length()").value(2));
    }

    @Test
    @DisplayName("POST /proposals/{id}/approval returns 409 when proposal not published")
    void initiateApproval_shouldReturn409_whenNotPublished() throws Exception {
        AuthContext auth = setupAuthenticatedUser();
        JpaBriefingSession briefing = createBriefingSession(auth.workspaceId());
        JpaProposal proposal = createDraftProposal(auth.workspaceId(), briefing.getId());
        // Proposal is still DRAFT

        String approvalBody = """
            {
              "approverEmails": ["client@example.com"]
            }
            """;

        mockMvc.perform(post("/proposals/" + proposal.getId() + "/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approvalBody)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }
}
