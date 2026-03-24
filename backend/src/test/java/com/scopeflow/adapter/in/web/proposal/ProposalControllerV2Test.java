package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.proposal.dto.CreateProposalRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

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
    @WithMockUser
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
    @WithMockUser
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
    @WithMockUser
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
}
