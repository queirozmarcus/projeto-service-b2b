package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.proposal.dto.ApproveProposalRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.core.domain.proposal.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ApprovalControllerV2 — public, no-auth endpoints.
 *
 * Uses @WebMvcTest slice with TestSecurityConfig to bypass JWT filter.
 */
@WebMvcTest(controllers = {ApprovalControllerV2.class})
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("ApprovalControllerV2")
class ApprovalControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProposalService proposalService;

    // ============ GET /proposals/{id}/approve ============

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 404 when proposal not found")
    void getApprovalPage_shouldReturn404_whenProposalNotFound() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        given(proposalService.findById(any(ProposalId.class)))
                .willReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/proposals/" + id + "/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 409 when proposal not PUBLISHED")
    void getApprovalPage_shouldReturn409_whenNotPublished() throws Exception {
        // Given — findById throws because service uses orElseThrow
        UUID id = UUID.randomUUID();
        given(proposalService.findById(any(ProposalId.class)))
                .willThrow(new ProposalNotFoundException("Proposal not found: " + id));

        // When / Then — controller's orElseThrow turns into 404
        mockMvc.perform(get("/proposals/" + id + "/approve"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Proposal Not Found"));
    }

    @Test
    @DisplayName("GET /proposals/{id}/approve returns 409 when proposal is DRAFT, not PUBLISHED")
    void getApprovalPage_shouldReturn409_whenProposalIsDraft() throws Exception {
        // Given — proposal exists but is DRAFT (not ProposalPublished)
        UUID id = UUID.randomUUID();
        ProposalDraft draft = Proposal.create(
                new com.scopeflow.core.domain.workspace.WorkspaceId(UUID.randomUUID()),
                UUID.randomUUID(),
                new com.scopeflow.core.domain.briefing.BriefingSessionId(UUID.randomUUID()),
                "Test Proposal"
        );
        given(proposalService.findById(any(ProposalId.class)))
                .willReturn(Optional.of(draft));

        // When / Then — controller throws InvalidProposalStateException → 409
        mockMvc.perform(get("/proposals/" + id + "/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    // ============ POST /proposals/{id}/approve ============

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 400 when approverName missing")
    void approve_shouldReturn400_whenApproverNameMissing() throws Exception {
        // Missing approverName
        UUID id = UUID.randomUUID();
        String body = "{\"approverEmail\": \"client@example.com\"}";

        mockMvc.perform(post("/proposals/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 400 when approverEmail invalid")
    void approve_shouldReturn400_whenApproverEmailInvalid() throws Exception {
        UUID id = UUID.randomUUID();
        ApproveProposalRequest request = new ApproveProposalRequest("John Client", "not-an-email");

        mockMvc.perform(post("/proposals/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 409 when proposal not in PUBLISHED state")
    void approve_shouldReturn409_whenNotPublished() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        willThrow(new InvalidProposalStateException("Proposal is not awaiting approval"))
                .given(proposalService).recordApproval(
                        any(ProposalId.class),
                        any(String.class),
                        any(String.class),
                        any(),
                        any()
                );

        ApproveProposalRequest request = new ApproveProposalRequest("John Client", "john@example.com");

        // When / Then
        mockMvc.perform(post("/proposals/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid Proposal State"));
    }

    @Test
    @DisplayName("POST /proposals/{id}/approve returns 201 on successful approval")
    void approve_shouldReturn201_whenValid() throws Exception {
        // Given — recordApproval does nothing (void)
        UUID id = UUID.randomUUID();
        ApproveProposalRequest request = new ApproveProposalRequest("John Client", "john@example.com");

        // When / Then
        mockMvc.perform(post("/proposals/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Forwarded-For", "203.0.113.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approverName").value("John Client"))
                .andExpect(jsonPath("$.approverEmail").value("john@example.com"))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.ipAddress").value("203.0.113.5"));
    }
}
