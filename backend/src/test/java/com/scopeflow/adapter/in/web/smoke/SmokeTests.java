package com.scopeflow.adapter.in.web.smoke;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.scopeflow.adapter.in.web.integration.ScopeFlowIntegrationTestBase;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end smoke tests for the full ScopeFlow user journey.
 *
 * Steps:
 *  1. Register user → 201 Created
 *  2. Login → TokenResponse
 *  3. Create workspace → 201 Created
 *  4. Invite member → 201 Created
 *  5. Create briefing → 201 Created
 *  6. Get briefing progress → 200 OK
 *  7. Complete briefing → 200 OK (pre-seeded data)
 *  8. Create proposal → 201 Created
 *  9. Publish proposal → 200 OK
 * 10. Initiate approval → 201 Created
 * 11. Approve (public) → 201 Created
 * 12. Get versions → 200 OK
 */
@DisplayName("Smoke Tests — Full user journey")
class SmokeTests extends ScopeFlowIntegrationTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Full ScopeFlow journey: register → login → workspace → briefing → proposal → approval")
    void fullUserJourney_shouldCompleteAllSteps() throws Exception {

        // ============ Step 1: Register user ============
        String registerBody = """
            {
              "email": "smoke@scopeflow.com",
              "password": "Password1!",
              "fullName": "Smoke Test User"
            }
            """;

        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("smoke@scopeflow.com"))
                .andReturn();

        JsonNode registerResponse = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String accessToken = registerResponse.get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        // ============ Step 2: Login with same credentials ============
        String loginBody = """
            {
              "email": "smoke@scopeflow.com",
              "password": "Password1!"
            }
            """;

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String loginToken = loginResponse.get("accessToken").asText();
        assertThat(loginToken).isNotBlank();

        // ============ Step 3: Create workspace ============
        MvcResult createWsResult = mockMvc.perform(post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Smoke Agency",
                              "niche": "social-media"
                            }
                            """)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Smoke Agency"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode wsResponse = objectMapper.readTree(createWsResult.getResponse().getContentAsString());
        UUID workspaceId = UUID.fromString(wsResponse.get("id").asText());
        assertThat(workspaceId).isNotNull();

        // Re-generate token with workspace_id (as would happen after workspace creation in real flow)
        // In the test, we need to get a token that includes the workspace_id claim
        // We'll get the user ID from DB to generate the token
        String userEmail = "smoke@scopeflow.com";
        var user = userRepository.findByEmailIgnoreCase(userEmail).orElseThrow();
        String tokenWithWorkspace = bearerToken(user.getId(), userEmail, workspaceId, "OWNER");

        // ============ Step 4: Invite member ============
        mockMvc.perform(post("/workspaces/" + workspaceId + "/members/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "client@example.com",
                              "role": "MEMBER"
                            }
                            """)
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("INVITED"));

        // ============ Step 5: Create briefing ============
        UUID clientId = UUID.randomUUID();
        MvcResult createBriefingResult = mockMvc.perform(post("/briefings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "clientId": "%s",
                              "serviceType": "SOCIAL_MEDIA"
                            }
                            """.formatted(clientId))
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn();

        JsonNode briefingResponse = objectMapper.readTree(createBriefingResult.getResponse().getContentAsString());
        UUID briefingId = UUID.fromString(briefingResponse.get("id").asText());

        // ============ Step 6: Get briefing progress ============
        mockMvc.perform(get("/briefings/" + briefingId + "/progress")
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.gaps").isArray());

        // ============ Step 7: Complete briefing (simulate via DB — smoke shortcut) ============
        // The briefing session was created in IN_PROGRESS. For the smoke test we delete it
        // and re-create it as COMPLETED to allow proposal creation without going through
        // the full answer scoring flow (which is unit-tested separately).
        briefingSessionRepository.deleteById(briefingId);
        JpaBriefingSession completedBriefing = new JpaBriefingSession(
                briefingId,
                workspaceId,
                clientId,
                "SOCIAL_MEDIA",
                "COMPLETED",
                UUID.randomUUID().toString(),
                95,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );
        briefingSessionRepository.save(completedBriefing);

        // Verify briefing is COMPLETED
        assertThat(briefingSessionRepository.findById(briefingId).orElseThrow().getStatus())
                .isEqualTo("COMPLETED");

        // ============ Step 8: Create proposal ============
        MvcResult createProposalResult = mockMvc.perform(post("/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "briefingId": "%s",
                              "clientId": "%s",
                              "proposalName": "Social Media Package Q2 2025"
                            }
                            """.formatted(briefingId, clientId))
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.proposalName").value("Social Media Package Q2 2025"))
                .andReturn();

        JsonNode proposalResponse = objectMapper.readTree(createProposalResult.getResponse().getContentAsString());
        UUID proposalId = UUID.fromString(proposalResponse.get("id").asText());

        // ============ Step 9: Publish proposal ============
        mockMvc.perform(post("/proposals/" + proposalId + "/publish")
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        // Verify DB state
        JpaProposal publishedProposal = proposalRepository.findById(proposalId).orElseThrow();
        assertThat(publishedProposal.getStatus()).isEqualTo("PUBLISHED");

        // ============ Step 10: Initiate approval ============
        MvcResult approvalWorkflowResult = mockMvc.perform(
                        post("/proposals/" + proposalId + "/approval")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "approverEmails": ["approver@example.com"]
                                    }
                                    """)
                                .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalId").value(proposalId.toString()))
                .andExpect(jsonPath("$.approvals").isArray())
                .andExpect(jsonPath("$.approvals.length()").value(1))
                .andReturn();

        // ============ Step 11: Approve (public endpoint — no JWT) ============
        mockMvc.perform(post("/proposals/" + proposalId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "approverName": "Jane Client",
                              "approverEmail": "approver@example.com"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approverName").value("Jane Client"))
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // Verify proposal transitioned to APPROVED
        JpaProposal approvedProposal = proposalRepository.findById(proposalId).orElseThrow();
        assertThat(approvedProposal.getStatus()).isEqualTo("APPROVED");

        // ============ Step 12: Get versions ============
        mockMvc.perform(get("/proposals/" + proposalId + "/versions")
                        .header("Authorization", tokenWithWorkspace))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
