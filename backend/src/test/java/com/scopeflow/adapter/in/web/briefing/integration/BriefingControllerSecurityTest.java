package com.scopeflow.adapter.in.web.briefing.integration;

import com.scopeflow.adapter.in.web.briefing.dto.CreateBriefingRequest;
import com.scopeflow.adapter.in.web.briefing.dto.PublicBriefingResponse;
import com.scopeflow.core.domain.briefing.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for security and multi-tenancy.
 *
 * Validates workspace isolation:
 * - Users can only see briefings in their own workspace
 * - Public token doesn't expose sensitive workspace data
 * - JWT claims are correctly extracted for workspace filtering
 *
 * Coverage: Workspace ownership, unauthorized access, data isolation.
 */
class BriefingControllerSecurityTest extends BriefingIntegrationTestBase {

    @Test
    void testWorkspaceOwnership_CanOnlySeeBriefingsInOwnWorkspace() throws Exception {
        // Given: user A creates briefing in workspace A
        var tokenA = generateTestJwtToken(WORKSPACE_ID_A, "userA@example.com");
        var sessionA = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        // And: user B in different workspace
        var tokenB = generateTestJwtToken(WORKSPACE_ID_B, "userB@example.com");

        // When: user B tries to get briefing from workspace A
        // Then: should return 403 Forbidden
        mockMvc.perform(get("/api/v1/briefings/{id}", sessionA.getId().value())
                        .header("Authorization", tokenB))
                .andExpect(status().isForbidden());

        // And: user A can access their own briefing
        mockMvc.perform(get("/api/v1/briefings/{id}", sessionA.getId().value())
                        .header("Authorization", tokenA))
                .andExpect(status().isOk());
    }

    @Test
    void testPublicTokenNoAuth_CorrectWorkspace() throws Exception {
        // Given: briefing in workspace A with public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: access via public token (no auth)
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then: response should not contain sensitive workspace info
        PublicBriefingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PublicBriefingResponse.class
        );

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("workspaceId");
        assertThat(responseBody).doesNotContain("clientId");
        assertThat(responseBody).doesNotContain(WORKSPACE_ID_A.toString());

        // But should contain non-sensitive data
        assertThat(response.serviceType()).isEqualTo(ServiceType.SOCIAL_MEDIA);
        assertThat(response.status()).isNotNull();
    }

    @Test
    void testJWTToken_ExtractedFromSecurityContext() throws Exception {
        // Given: authenticated users in different workspaces
        var tokenA = generateTestJwtToken(WORKSPACE_ID_A, "userA@example.com");
        var tokenB = generateTestJwtToken(WORKSPACE_ID_B, "userB@example.com");

        // When: user A creates briefing
        var request = new CreateBriefingRequest(UUID.randomUUID(), ServiceType.SOCIAL_MEDIA);
        MvcResult resultA = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then: briefing should be created in workspace A
        var sessionA = sessionRepository.findAll().stream()
                .filter(s -> s.getWorkspaceId().equals(WORKSPACE_ID_A))
                .findFirst()
                .orElseThrow();
        assertThat(sessionA.getWorkspaceId()).isEqualTo(WORKSPACE_ID_A);

        // When: user B creates briefing
        MvcResult resultB = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then: briefing should be created in workspace B
        var sessionB = sessionRepository.findAll().stream()
                .filter(s -> s.getWorkspaceId().equals(WORKSPACE_ID_B))
                .findFirst()
                .orElseThrow();
        assertThat(sessionB.getWorkspaceId()).isEqualTo(WORKSPACE_ID_B);

        // Verify isolation: user A cannot see user B's briefings
        mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + sessionB.getId() + "')]").doesNotExist());

        // Verify isolation: user B cannot see user A's briefings
        mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '" + sessionA.getId() + "')]").doesNotExist());
    }
}
