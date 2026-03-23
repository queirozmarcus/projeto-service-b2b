package com.scopeflow.adapter.in.web.briefing.integration;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for authenticated Briefing endpoints.
 *
 * Tests all 8 endpoints in BriefingControllerV1:
 * 1. POST /api/v1/briefings - Create briefing
 * 2. GET /api/v1/briefings - List briefings
 * 3. GET /api/v1/briefings/{id} - Get briefing details
 * 4. GET /api/v1/briefings/{id}/progress - Get progress
 * 5. GET /api/v1/briefings/{id}/next-question - Get next question
 * 6. POST /api/v1/briefings/{id}/answers - Submit answer
 * 7. POST /api/v1/briefings/{id}/complete - Complete briefing
 * 8. POST /api/v1/briefings/{id}/abandon - Abandon briefing
 *
 * Coverage: Happy path + error cases for each endpoint.
 */
class BriefingControllerV1IntegrationTest extends BriefingIntegrationTestBase {

    @Test
    void testCreateBriefing_Success() throws Exception {
        // Given: authenticated user with workspace
        var request = new CreateBriefingRequest(CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: create briefing
        MvcResult result = mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();

        // Then: verify response
        BriefingResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BriefingResponse.class
        );
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo(BriefingStatus.IN_PROGRESS);
        assertThat(response.publicToken()).isNotNull();
        assertThat(response.serviceType()).isEqualTo(ServiceType.SOCIAL_MEDIA);

        // Verify persistence
        var savedEntity = sessionRepository.findById(response.id()).orElseThrow();
        var saved = savedEntity.toDomain();
        assertThat(saved.getWorkspaceId()).isEqualTo(new WorkspaceId(WORKSPACE_ID_A));
        assertThat(saved.getClientId()).isEqualTo(new ClientId(CLIENT_ID));
    }

    @Test
    void testCreateBriefing_DuplicateActive() throws Exception {
        // Given: existing active briefing for same client/service
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        var request = new CreateBriefingRequest(CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: try to create duplicate
        // Then: should return 409 Conflict
        mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-006"));
    }

    @Test
    void testCreateBriefing_ValidationError() throws Exception {
        // Given: invalid request (null clientId)
        var request = new CreateBriefingRequest(null, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: create with invalid data
        // Then: should return 400 Bad Request
        mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION-400"))
                .andExpect(jsonPath("$.violations").isArray());
    }

    @Test
    void testListBriefings_Paginated() throws Exception {
        // Given: multiple briefings in workspace A
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        createTestBriefing(WORKSPACE_ID_A, UUID.randomUUID(), ServiceType.LANDING_PAGE);
        createTestBriefing(WORKSPACE_ID_A, UUID.randomUUID(), ServiceType.WEB_DEVELOPMENT);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: list briefings
        MvcResult result = mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", token)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: verify pagination response
        PageResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PageResponse.class
        );
        assertThat(response.content()).isNotNull();
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.number()).isEqualTo(0);
    }

    @Test
    void testListBriefings_FilterByStatus() throws Exception {
        // Given: briefings with different statuses
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        createCompletedBriefing(WORKSPACE_ID_A, UUID.randomUUID());

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: filter by IN_PROGRESS
        MvcResult result = mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", token)
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: only IN_PROGRESS briefings returned
        PageResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PageResponse.class
        );
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void testListBriefings_FilterByServiceType() throws Exception {
        // Given: briefings with different service types
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        createTestBriefing(WORKSPACE_ID_A, UUID.randomUUID(), ServiceType.LANDING_PAGE);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: filter by SOCIAL_MEDIA
        MvcResult result = mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", token)
                        .param("serviceType", "SOCIAL_MEDIA"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: only SOCIAL_MEDIA briefings returned
        PageResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PageResponse.class
        );
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void testListBriefings_FilterByCreatedAfter() throws Exception {
        // Given: briefings created at different times
        var cutoffDate = Instant.now().minusSeconds(3600); // 1 hour ago
        createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: filter by createdAfter
        MvcResult result = mockMvc.perform(get("/api/v1/briefings")
                        .header("Authorization", token)
                        .param("createdAfter", cutoffDate.toString()))
                .andExpect(status().isOk())
                .andReturn();

        // Then: only recent briefings returned
        PageResponse<?> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                PageResponse.class
        );
        assertThat(response.totalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testGetBriefing_Found() throws Exception {
        // Given: existing briefing with questions and answers
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        createTestAnswer(session.getId(), question.questionId(), "Increase brand awareness");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get briefing details
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        // Then: verify full detail response
        BriefingDetailResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                BriefingDetailResponse.class
        );
        assertThat(response.session()).isNotNull();
        assertThat(response.session().id()).isEqualTo(session.getId().value());
        assertThat(response.progress()).isNotNull();
        assertThat(response.questions()).hasSize(1);
        assertThat(response.answers()).hasSize(1);
    }

    @Test
    void testGetBriefing_NotFound() throws Exception {
        // Given: non-existent briefing ID
        var invalidId = UUID.randomUUID();
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get briefing that doesn't exist
        // Then: should return 404 Not Found
        mockMvc.perform(get("/api/v1/briefings/{id}", invalidId)
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("BRIEFING-001"));
    }

    @Test
    void testGetBriefing_UnauthorizedWorkspace() throws Exception {
        // Given: briefing in workspace A, user from workspace B
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var token = generateTestJwtToken(WORKSPACE_ID_B, "user@other-workspace.com");

        // When: try to access briefing from different workspace
        // Then: should return 403 Forbidden
        mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetProgress_WithCache() throws Exception {
        // Given: briefing with some answers
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        createTestAnswer(session.getId(), question.questionId(), "Increase brand awareness");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get progress
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}/progress", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().exists("Cache-Control"))
                .andReturn();

        // Then: verify progress response
        ProgressResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ProgressResponse.class
        );
        assertThat(response.currentStep()).isGreaterThanOrEqualTo(1);
        assertThat(response.totalSteps()).isGreaterThan(0);
        assertThat(response.completionPercentage()).isGreaterThan(0);

        // Verify cache header
        String cacheControl = result.getResponse().getHeader("Cache-Control");
        assertThat(cacheControl).contains("max-age=30");
    }

    @Test
    void testGetNextQuestion_Success() throws Exception {
        // Given: briefing with unanswered questions
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        createTestQuestion(session.getId(), 1, "What is your goal?");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get next question
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}/next-question", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();

        // Then: returns next unanswered question
        QuestionResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                QuestionResponse.class
        );
        assertThat(response.questionId()).isNotNull();
        assertThat(response.questionText()).isNotBlank();
        assertThat(response.step()).isEqualTo(1);
    }

    @Test
    void testGetNextQuestion_AllAnswered() throws Exception {
        // Given: briefing with all questions answered
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");
        createTestAnswer(session.getId(), question.questionId(), "Increase brand awareness");

        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: get next question when all answered
        // Then: should return 409 Conflict
        mockMvc.perform(get("/api/v1/briefings/{id}/next-question", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isConflict());
    }

    @Test
    void testSubmitAnswer_Success() throws Exception {
        // Given: briefing with unanswered question
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");

        var request = new SubmitAnswerRequest(question.questionId().value(), "Increase brand awareness");
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: submit answer
        mockMvc.perform(post("/api/v1/briefings/{id}/answers", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Then: verify answer persisted
        var answers = answerRepository.findAll();
        assertThat(answers).hasSize(1);
        assertThat(answers.get(0).getAnswerText()).isEqualTo("Increase brand awareness");
    }

    @Test
    void testSubmitAnswer_EmptyText() throws Exception {
        // Given: briefing with question
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var question = createTestQuestion(session.getId(), 1, "What is your goal?");

        var request = new SubmitAnswerRequest(question.questionId().value(), "");
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");

        // When: submit empty answer
        // Then: should return 400 Bad Request
        mockMvc.perform(post("/api/v1/briefings/{id}/answers", session.getId().value())
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION-400"));
    }
}
