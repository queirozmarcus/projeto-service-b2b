package com.scopeflow.adapter.in.web.proposal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.proposal.dto.ApproveProposalRequest;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.core.domain.proposal.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for I1: IP spoofing mitigation in ApprovalControllerV2.
 *
 * Tests: trusted proxy accepts X-Forwarded-For, untrusted proxy ignores header,
 * chain format, invalid IP rejected, correct IP stored in approval response.
 */
@WebMvcTest(ApprovalControllerV2.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@DisplayName("ApprovalControllerV2 — IP spoofing mitigation (I1)")
class IpSpoofingMitigationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProposalService proposalService;

    private static final UUID PROPOSAL_ID = UUID.randomUUID();
    private static final String APPROVE_URL = "/proposals/" + PROPOSAL_ID + "/approve";

    private void stubPublishedProposal() {
        ProposalPublished published = new ProposalPublished(
                ProposalId.of(PROPOSAL_ID),
                new com.scopeflow.core.domain.workspace.WorkspaceId(UUID.randomUUID()),
                UUID.randomUUID(),
                new com.scopeflow.core.domain.briefing.BriefingSessionId(UUID.randomUUID()),
                "Test Proposal",
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );
        given(proposalService.findById(ProposalId.of(PROPOSAL_ID)))
                .willReturn(Optional.of(published));
    }

    private String approvalBody() throws Exception {
        return objectMapper.writeValueAsString(
                new ApproveProposalRequest("Client Name", "client@example.com")
        );
    }

    @Nested
    @DisplayName("Trusted proxy — accepts X-Forwarded-For")
    class TrustedProxyTests {

        @Test
        @DisplayName("Loopback remoteAddr + X-Forwarded-For → uses forwarded IP")
        void loopback_shouldUseForwardedIp() throws Exception {
            stubPublishedProposal();

            // The client forwards from localhost (127.0.0.1) which is trusted
            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            })
                            .header("X-Forwarded-For", "203.0.113.10"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("203.0.113.10"));
        }

        @Test
        @DisplayName("Private range 10.x remoteAddr + X-Forwarded-For → uses forwarded IP")
        void privateRange10_shouldUseForwardedIp() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.5");
                                return request;
                            })
                            .header("X-Forwarded-For", "198.51.100.25"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("198.51.100.25"));
        }

        @Test
        @DisplayName("Private range 192.168.x remoteAddr + X-Forwarded-For → uses forwarded IP")
        void privateRange192_shouldUseForwardedIp() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("192.168.1.100");
                                return request;
                            })
                            .header("X-Forwarded-For", "203.0.113.50"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("203.0.113.50"));
        }

        @Test
        @DisplayName("X-Forwarded-For chain (client, proxy1, proxy2) → takes leftmost IP")
        void forwardedForChain_shouldTakeLeftmostIp() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.1");
                                return request;
                            })
                            .header("X-Forwarded-For", "203.0.113.1, 10.0.0.2, 10.0.0.3"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("203.0.113.1"));
        }
    }

    @Nested
    @DisplayName("Untrusted proxy — ignores X-Forwarded-For")
    class UntrustedProxyTests {

        @Test
        @DisplayName("Public remoteAddr + X-Forwarded-For → ignores header, uses remoteAddr")
        void publicRemoteAddr_shouldIgnoreForwardedFor() throws Exception {
            stubPublishedProposal();

            // remoteAddr is a public IP (not trusted proxy)
            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("198.51.100.99");
                                return request;
                            })
                            .header("X-Forwarded-For", "1.2.3.4"))
                    .andExpect(status().isCreated())
                    // Should use the actual remoteAddr, not the spoofed X-Forwarded-For
                    .andExpect(jsonPath("$.ipAddress").value("198.51.100.99"));
        }
    }

    @Nested
    @DisplayName("Invalid IP format — fallback to remoteAddr")
    class InvalidIpFormatTests {

        @Test
        @DisplayName("Malformed X-Forwarded-For value → falls back to remoteAddr")
        void malformedForwardedFor_shouldFallbackToRemoteAddr() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            })
                            // Injected script tag — should be rejected as invalid IP
                            .header("X-Forwarded-For", "<script>alert(1)</script>"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("127.0.0.1"));
        }

        @Test
        @DisplayName("Blank X-Forwarded-For → falls back to remoteAddr")
        void blankForwardedFor_shouldFallbackToRemoteAddr() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("127.0.0.1");
                                return request;
                            })
                            .header("X-Forwarded-For", "   "))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").value("127.0.0.1"));
        }
    }

    @Nested
    @DisplayName("IP stored in approval response")
    class IpInApprovalResponseTests {

        @Test
        @DisplayName("Approval response contains the resolved client IP address")
        void approvalResponse_shouldContainResolvedIp() throws Exception {
            stubPublishedProposal();

            mockMvc.perform(post(APPROVE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(approvalBody())
                            .with(request -> {
                                request.setRemoteAddr("10.0.0.1");
                                return request;
                            })
                            .header("X-Forwarded-For", "203.0.113.77"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.ipAddress").isNotEmpty())
                    .andExpect(jsonPath("$.ipAddress").value("203.0.113.77"))
                    .andExpect(jsonPath("$.approverName").value("Client Name"))
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        }
    }
}
