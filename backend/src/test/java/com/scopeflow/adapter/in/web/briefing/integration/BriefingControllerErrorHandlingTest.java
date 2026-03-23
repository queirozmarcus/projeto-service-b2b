package com.scopeflow.adapter.in.web.briefing.integration;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.core.domain.briefing.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for error handling and exception mapping.
 *
 * Validates RFC 9457 (Problem Details) format for all error responses:
 * - Standard fields: type, title, status, detail, instance
 * - Custom fields: errorCode, errorId, timestamp
 *
 * Coverage: All 7 custom exceptions + standard HTTP errors.
 */
class BriefingControllerErrorHandlingTest extends BriefingIntegrationTestBase {

    @Test
    void testErrorResponse_BriefingNotFound() throws Exception {
        // Given: non-existent briefing ID
        var invalidId = UUID.randomUUID();
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get briefing that doesn't exist
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", invalidId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then: verify RFC 9457 format
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getTitle()).contains("Briefing Not Found");
        assertThat(error.getInstance()).isNotNull();

        // Custom fields
        Map<String, Object> properties = error.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties.get("errorCode")).isEqualTo("BRIEFING-001");
        assertThat(properties.get("errorId")).isNotNull();
        assertThat(properties.get("timestamp")).isNotNull();
    }

    @Test
    void testErrorResponse_BriefingAlreadyCompleted() throws Exception {
        // Given: completed briefing
        var completed = createCompletedBriefing(WORKSPACE_ID_A, CLIENT_ID);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: try to complete again
        var request = new CompleteBriefingRequest(95, java.util.List.of());
        MvcResult result = mockMvc.perform(post("/api/v1/briefings/{id}/complete", completed.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(409);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-002");
    }

    @Test
    void testErrorResponse_InvalidAnswer() throws Exception {
        // Given: briefing with question
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: submit invalid answer (too short, e.g., "a")
        var request = new SubmitAnswerRequest(question.questionId().value(), "a");
        MvcResult result = mockMvc.perform(post("/api/v1/briefings/{id}/answers", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-003");
    }

    @Test
    void testErrorResponse_MaxFollowupExceeded() throws Exception {
        // Given: briefing with question that already has max follow-ups
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        // Simulate max follow-ups already created
        createTestAnswer(session.getId(), question.questionId(), "First answer");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: try to submit another follow-up (exceeds limit)
        var request = new SubmitAnswerRequest(question.questionId().value(), "Follow-up answer");
        MvcResult result = mockMvc.perform(post("/api/v1/briefings/{id}/answers", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(422);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-004");
    }

    @Test
    void testErrorResponse_IncompleteGaps() throws Exception {
        // Given: briefing with low completion score
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: try to complete with score < 80
        var request = new CompleteBriefingRequest(65, java.util.List.of("Need more details"));
        MvcResult result = mockMvc.perform(post("/api/v1/briefings/{id}/complete", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(422);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-005");
    }

    @Test
    void testErrorResponse_BriefingAlreadyInProgress() throws Exception {
        // Given: existing active briefing for same client/service
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: try to create duplicate
        var request = new CreateBriefingRequest(CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        MvcResult result = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(409);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-006");
    }

    @Test
    void testErrorResponse_PublicTokenInvalid() throws Exception {
        // Given: invalid public token
        var invalidToken = UUID.randomUUID();

        // When: access public endpoint with invalid token
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", invalidToken))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(404);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("BRIEFING-007");
    }

    @Test
    void testErrorResponse_Unauthorized() throws Exception {
        // Given: request without JWT token
        var briefingId = UUID.randomUUID();

        // When: access authenticated endpoint without token
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", briefingId))
                .andExpect(status().isUnauthorized())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(401);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("AUTH-401");
    }

    @Test
    void testErrorResponse_ValidationError() throws Exception {
        // Given: invalid request (null clientId)
        var request = new CreateBriefingRequest(null, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: create with validation error
        MvcResult result = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then: verify error response includes violations
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(400);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("VALIDATION-400");
        assertThat(error.getProperties().get("violations")).isNotNull();
    }

    @Test
    void testErrorResponse_HasErrorId() throws Exception {
        // Given: any error scenario
        var invalidId = UUID.randomUUID();
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: trigger error
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", invalidId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then: verify errorId is unique UUID
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        Object errorId = error.getProperties().get("errorId");
        assertThat(errorId).isNotNull();
        assertThat(errorId.toString()).matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }

    @Test
    void testErrorResponse_HasTimestamp() throws Exception {
        // Given: any error scenario
        var invalidId = UUID.randomUUID();
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: trigger error
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", invalidId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andReturn();

        // Then: verify timestamp exists
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        Object timestamp = error.getProperties().get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp.toString()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
    }

    @Test
    void testErrorResponse_RateLimitExceeded() throws Exception {
        // Given: briefing with valid public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: exceed rate limit (10 req/min for public endpoints)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                    .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isTooManyRequests())
                .andReturn();

        // Then: verify error response
        ProblemDetail error = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProblemDetail.class
        );
        assertThat(error.getStatus()).isEqualTo(429);
        assertThat(error.getProperties().get("errorCode")).isEqualTo("RATE-429");
    }
}
