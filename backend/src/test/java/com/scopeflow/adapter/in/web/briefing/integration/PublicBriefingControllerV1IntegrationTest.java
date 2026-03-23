package com.scopeflow.adapter.in.web.briefing.integration;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.core.domain.briefing.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for public (no auth) Briefing endpoints.
 *
 * Tests all 3 endpoints in PublicBriefingControllerV1:
 * 1. GET /public/briefings/{publicToken} - Get public briefing
 * 2. GET /public/briefings/{publicToken}/next-question - Get next question (public)
 * 3. POST /public/briefings/{publicToken}/answers - Submit answer (public)
 *
 * Coverage: Happy path + error cases + rate limiting.
 */
class PublicBriefingControllerV1IntegrationTest extends BriefingIntegrationTestBase {

    @Test
    void testGetPublicBriefing_Success() throws Exception {
        // Given: briefing with public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: get briefing via public token (no auth)
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then: verify public response (no sensitive data)
        PublicBriefingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PublicBriefingResponse.class
        );
        assertThat(response.serviceType()).isEqualTo(ServiceType.SOCIAL_MEDIA);
        assertThat(response.status()).isEqualTo(BriefingStatus.IN_PROGRESS);
        // Public response should not expose workspaceId or clientId
        assertThat(result.getResponse().getContentAsString()).doesNotContain("workspaceId");
        assertThat(result.getResponse().getContentAsString()).doesNotContain("clientId");
    }

    @Test
    void testGetPublicBriefing_InvalidToken() throws Exception {
        // Given: invalid public token
        var invalidToken = UUID.randomUUID();

        // When: get briefing with invalid token
        // Then: should return 404 Not Found
        mockMvc.perform(get("/public/briefings/{publicToken}", invalidToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-007"));
    }

    @Test
    void testGetPublicNextQuestion_Success() throws Exception {
        // Given: briefing with unanswered questions
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        var publicToken = session.getPublicToken().value();

        // When: get next question via public token (no auth)
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}/next-question", publicToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then: returns next question
        QuestionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuestionResponse.class
        );
        assertThat(response.questionId()).isEqualTo(question.questionId().value());
        assertThat(response.questionText()).isEqualTo("What is your goal?");
        assertThat(response.step()).isEqualTo(1);
    }

    @Test
    void testGetPublicNextQuestion_InvalidToken() throws Exception {
        // Given: invalid public token
        var invalidToken = UUID.randomUUID();

        // When: get next question with invalid token
        // Then: should return 404 Not Found
        mockMvc.perform(get("/public/briefings/{publicToken}/next-question", invalidToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-007"));
    }

    @Test
    void testGetPublicNextQuestion_AllAnswered() throws Exception {
        // Given: briefing with all questions answered
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        createTestAnswer(session.getId(), question.questionId(), "Increase brand awareness");
        var publicToken = session.getPublicToken().value();

        // When: get next question when all answered
        // Then: should return 409 Conflict
        mockMvc.perform(get("/public/briefings/{publicToken}/next-question", publicToken))
                .andExpect(status().isConflict());
    }

    @Test
    void testSubmitPublicAnswer_Success() throws Exception {
        // Given: briefing with unanswered question
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        var publicToken = session.getPublicToken().value();

        var request = new SubmitAnswerRequest(question.questionId().value(), "Increase brand awareness");

        // When: submit answer via public token (no auth)
        mockMvc.perform(post("/public/briefings/{publicToken}/answers", publicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then: verify answer persisted
        var answers = answerRepository.findAll();
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getAnswerText()).isEqualTo("Increase brand awareness");
    }

    @Test
    void testSubmitPublicAnswer_InvalidToken() throws Exception {
        // Given: invalid public token
        var invalidToken = UUID.randomUUID();
        var request = new SubmitAnswerRequest(UUID.randomUUID(), "Some answer");

        // When: submit answer with invalid token
        // Then: should return 404 Not Found
        mockMvc.perform(post("/public/briefings/{publicToken}/answers", invalidToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-007"));
    }

    @Test
    void testSubmitPublicAnswer_EmptyText() throws Exception {
        // Given: briefing with question
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        var publicToken = session.getPublicToken().value();

        var request = new SubmitAnswerRequest(question.questionId().value(), "");

        // When: submit empty answer
        // Then: should return 400 Bad Request
        mockMvc.perform(post("/public/briefings/{publicToken}/answers", publicToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION-400"));
    }

    @Test
    void testPublicEndpoint_RateLimited() throws Exception {
        // Given: briefing with valid public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: send 11 requests in quick succession (rate limit: 10 req/min)
        // Then: 11th request should return 429 Too Many Requests
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                    .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE-429"));
    }

    @Test
    void testPublicEndpoint_RateLimitHeader() throws Exception {
        // Given: briefing with valid public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: make first request
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isOk())
                .andReturn();

        // Then: verify rate limit header present
        String rateLimitHeader = result.getResponse().getHeader("X-Rate-Limit-Remaining");
        assertThat(rateLimitHeader).isNotNull();
        assertThat(Integer.parseInt(rateLimitHeader)).isLessThanOrEqualTo(9); // 10 - 1 request
    }
}
