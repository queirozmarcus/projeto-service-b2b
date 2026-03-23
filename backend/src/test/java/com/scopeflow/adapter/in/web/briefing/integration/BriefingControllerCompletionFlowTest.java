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
 * End-to-end flow tests for briefing completion.
 *
 * Tests complete briefing lifecycle:
 * - Create → Answer questions → Complete
 * - Create → Abandon
 * - Error cases (low score, already completed)
 *
 * Coverage: Multi-step workflows with state transitions.
 */
class BriefingControllerCompletionFlowTest extends BriefingIntegrationTestBase {

    @Test
    void testCompleteFlow_CreateAnswerComplete() throws Exception {
        // Given: authenticated user
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // Step 1: Create briefing
        var createRequest = new CreateBriefingRequest(CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        MvcResult createResult = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        BriefingResponse briefing = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                BriefingResponse.class
        );
        assertThat(briefing.status()).isEqualTo(BriefingStatus.IN_PROGRESS);

        // Step 2: Get next question
        MvcResult questionResult = mockMvc.perform(get("/api/v1/briefings/{id}/next-question", briefing.id())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        QuestionResponse question = objectMapper.readValue(
                questionResult.getResponse().getContentAsString(),
                QuestionResponse.class
        );

        // Step 3: Submit answer
        var answerRequest = new SubmitAnswerRequest(question.questionId(), "Comprehensive answer to the question");
        mockMvc.perform(post("/api/v1/briefings/{id}/answers", briefing.id())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(answerRequest)))
                .andExpect(status().isNoContent());

        // Step 4: Complete briefing (assume score >= 80)
        var completeRequest = new CompleteBriefingRequest(95, java.util.List.of());
        MvcResult completeResult = mockMvc.perform(post("/api/v1/briefings/{id}/complete", briefing.id())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Then: verify state transitions
        BriefingResponse completed = objectMapper.readValue(
                completeResult.getResponse().getContentAsString(),
                BriefingResponse.class
        );
        assertThat(completed.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(completed.completionScore()).isEqualTo(95);
    }

    @Test
    void testCompleteBriefing_Success() throws Exception {
        // Given: briefing with sufficient completion score
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        createTestAnswer(session.getId(), question.questionId(), "Increase brand awareness");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var request = new CompleteBriefingRequest(95, java.util.List.of());

        // When: complete briefing with score >= 80
        MvcResult result = mockMvc.perform(post("/api/v1/briefings/{id}/complete", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        // Then: status changes to COMPLETED
        BriefingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BriefingResponse.class
        );
        assertThat(response.status()).isEqualTo(BriefingStatus.COMPLETED);
        assertThat(response.completionScore()).isEqualTo(95);

        // Verify persistence
        var savedEntity = sessionRepository.findById(response.id()).orElseThrow();
        var saved = savedEntity.toDomain();
        assertThat(saved).isInstanceOf(BriefingCompleted.class);
        assertThat(((BriefingCompleted) saved).getCompletionScore().score()).isEqualTo(95);
    }

    @Test
    void testCompleteBriefing_LowScore() throws Exception {
        // Given: briefing with low completion score
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var request = new CompleteBriefingRequest(65, java.util.List.of("Need more details on timeline"));

        // When: try to complete with score < 80
        // Then: should return 422 Unprocessable Entity
        mockMvc.perform(post("/api/v1/briefings/{id}/complete", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-005"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("80")));
    }

    @Test
    void testAbandonBriefing_Success() throws Exception {
        // Given: active briefing
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var request = new AbandonBriefingRequest("Client decided to postpone");

        // When: abandon briefing
        mockMvc.perform(post("/api/v1/briefings/{id}/abandon", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then: status changes to ABANDONED
        var savedEntity = sessionRepository.findById(session.getId().value()).orElseThrow();
        var saved = savedEntity.toDomain();
        assertThat(saved).isInstanceOf(BriefingAbandoned.class);
    }

    @Test
    void testAbandonBriefing_CanStartNew() throws Exception {
        // Given: abandoned briefing
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var abandonRequest = new AbandonBriefingRequest("Test reason");

        // Step 1: Abandon existing briefing
        mockMvc.perform(post("/api/v1/briefings/{id}/abandon", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(abandonRequest)))
                .andExpect(status().isNoContent());

        // Step 2: Create new briefing for same client/service
        var createRequest = new CreateBriefingRequest(CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        MvcResult result = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then: new briefing allowed
        BriefingResponse newBriefing = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BriefingResponse.class
        );
        assertThat(newBriefing.id()).isNotEqualTo(session.getId().value());
        assertThat(newBriefing.status()).isEqualTo(BriefingStatus.IN_PROGRESS);
    }
}
