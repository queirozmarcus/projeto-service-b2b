package com.scopeflow.adapter.in.web.briefing.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.scopeflow.adapter.in.web.integration.ScopeFlowIntegrationTestBase;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswerSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfile;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfileSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestion;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestionSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.scopeflow.adapter.in.web.briefing.fixtures.BriefingSessionTestFixtures.answersJson;
import static com.scopeflow.adapter.in.web.briefing.fixtures.BriefingSessionTestFixtures.singleAnswerJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for BriefingSessionControllerV2.
 * Uses Testcontainers PostgreSQL with all Flyway migrations applied.
 *
 * Covers:
 * - POST   /api/v1/proposals/{id}/briefing-sessions
 * - GET    /api/v1/briefing-sessions/{id}
 * - GET    /api/v1/briefing-sessions/{id}/questions
 * - POST   /api/v1/briefing-sessions/{id}/answers
 * - POST   /api/v1/briefing-sessions/{id}/complete
 * - GET    /api/v1/briefing-sessions/token/{token}
 * - Public: GET  /public/briefings/{token}/questions
 * - Public: POST /public/briefings/{token}/batch-answers
 */
class BriefingSessionControllerV2IntegrationTest extends ScopeFlowIntegrationTestBase {

    @Autowired
    private JpaServiceContextProfileSpringRepository profileRepo;

    @Autowired
    private JpaServiceContextQuestionSpringRepository questionRepo;

    @Autowired
    private JpaBriefingAnswerSpringRepository answerRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Shared test context for the happy-path tests
    private AuthContext auth;
    private JpaProposal proposal;
    private JpaBriefingSession linkedBriefing;

    @BeforeEach
    void setUpBriefingData() {
        // Defensively clear briefing-specific data before base cleanDatabase() runs,
        // in case a previous test left answers behind (trigger-protected table).
        jdbcTemplate.execute("ALTER TABLE briefing_answers DISABLE TRIGGER briefing_answers_immutable_trigger");
        jdbcTemplate.execute("TRUNCATE TABLE briefing_answers CASCADE");
        jdbcTemplate.execute("ALTER TABLE briefing_answers ENABLE TRIGGER briefing_answers_immutable_trigger");

        auth = setupAuthenticatedUser();
        linkedBriefing = createBriefingSession(auth.workspaceId());
        proposal = createDraftProposal(auth.workspaceId(), linkedBriefing.getId());
    }

    /**
     * Clean briefing-specific tables after each test.
     *
     * briefing_answers has an immutable trigger (BEFORE DELETE raises exception).
     * We bypass it using ALTER TABLE ... DISABLE TRIGGER, which is safe in test scope
     * because the data is throw-away anyway.
     * service_context_questions and service_context_profiles have no such restriction.
     */
    @AfterEach
    void cleanBriefingData() {
        jdbcTemplate.execute("ALTER TABLE briefing_answers DISABLE TRIGGER briefing_answers_immutable_trigger");
        jdbcTemplate.execute("TRUNCATE TABLE briefing_answers CASCADE");
        jdbcTemplate.execute("ALTER TABLE briefing_answers ENABLE TRIGGER briefing_answers_immutable_trigger");
        questionRepo.deleteAll();
        profileRepo.deleteAll();
    }

    // ============ POST /proposals/{id}/briefing-sessions ============

    @Nested
    @DisplayName("POST /api/v1/proposals/{proposalId}/briefing-sessions")
    class CreateBriefingSession {

        @Test
        @DisplayName("should create briefing session and return 201 with session data")
        void shouldCreate_andReturn201() throws Exception {
            mockMvc.perform(post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andExpect(jsonPath("$.publicToken").isNotEmpty())
                    .andExpect(jsonPath("$.id").isNotEmpty());
        }

        @Test
        @DisplayName("should return 401 without authentication")
        void shouldReturn401_withoutAuth() throws Exception {
            mockMvc.perform(post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should return 403 when proposal belongs to another workspace")
        void shouldReturn403_whenWrongWorkspace() throws Exception {
            // Create proposal owned by a different workspace
            AuthContext other = setupSecondUser();
            JpaBriefingSession otherBriefing = createBriefingSession(other.workspaceId());
            JpaProposal otherProposal = createDraftProposal(other.workspaceId(), otherBriefing.getId());

            // auth (workspace A) tries to create a briefing session on workspace B's proposal
            mockMvc.perform(post("/api/v1/proposals/{id}/briefing-sessions", otherProposal.getId())
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should return 404 when proposal does not exist")
        void shouldReturn404_whenProposalNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/proposals/{id}/briefing-sessions", UUID.randomUUID())
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isNotFound());
        }
    }

    // ============ GET /briefing-sessions/{id} ============

    @Nested
    @DisplayName("GET /api/v1/briefing-sessions/{id}")
    class GetBriefingSession {

        @Test
        @DisplayName("should return session details for owner")
        void shouldReturnSessionDetails() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            mockMvc.perform(get("/api/v1/briefing-sessions/{id}", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sessionId))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("should return 403 when session belongs to another workspace")
        void shouldReturn403_wrongWorkspace() throws Exception {
            AuthContext other = setupSecondUser();
            JpaBriefingSession otherBriefing = createBriefingSession(other.workspaceId());
            JpaProposal otherProposal = createDraftProposal(other.workspaceId(), otherBriefing.getId());
            String otherSessionId = createSessionViaApi(other, otherProposal.getId());

            // Workspace A tries to read workspace B's session
            mockMvc.perform(get("/api/v1/briefing-sessions/{id}", otherSessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ GET /briefing-sessions/{id}/questions ============

    @Nested
    @DisplayName("GET /api/v1/briefing-sessions/{id}/questions")
    class GetQuestions {

        @Test
        @DisplayName("should return empty list when no ServiceContextProfile configured")
        void shouldReturnEmptyList_whenNoProfile() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            mockMvc.perform(get("/api/v1/briefing-sessions/{id}/questions", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("should return questions from active ServiceContextProfile ordered by orderIndex")
        void shouldReturnQuestions_whenProfileExists() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            createServiceContextQuestion(profile.getId(), "What are your goals?", 1, true);
            createServiceContextQuestion(profile.getId(), "Who is your audience?", 2, false);

            mockMvc.perform(get("/api/v1/briefing-sessions/{id}/questions", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].orderIndex").value(1))
                    .andExpect(jsonPath("$[0].required").value(true))
                    .andExpect(jsonPath("$[1].orderIndex").value(2))
                    .andExpect(jsonPath("$[1].required").value(false));
        }

        @Test
        @DisplayName("should return 403 when session belongs to another workspace")
        void shouldReturn403_wrongWorkspace() throws Exception {
            AuthContext other = setupSecondUser();
            JpaBriefingSession otherBriefing = createBriefingSession(other.workspaceId());
            JpaProposal otherProposal = createDraftProposal(other.workspaceId(), otherBriefing.getId());
            String otherSessionId = createSessionViaApi(other, otherProposal.getId());

            mockMvc.perform(get("/api/v1/briefing-sessions/{id}/questions", otherSessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ POST /briefing-sessions/{id}/answers ============

    @Nested
    @DisplayName("POST /api/v1/briefing-sessions/{id}/answers")
    class SubmitAnswers {

        @Test
        @DisplayName("should return 204 and persist answers")
        void shouldReturn204_andPersistAnswers() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "My answer to Q1")))
                    .andExpect(status().isNoContent());

            assertThat(answerRepo.findAll()).anyMatch(a -> a.getQuestionId().equals(q.getId()));
        }

        @Test
        @DisplayName("should return 400 when answers list is empty")
        void shouldReturn400_whenAnswersEmpty() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"answers\": []}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should be idempotent — submitting same question twice produces one answer row")
        void shouldBeIdempotent_skipDuplicate() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);

            String body = singleAnswerJson(q.getId(), "First answer");

            // Submit twice — both requests must succeed
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNoContent());

            // Only one row persisted
            long count = answerRepo.findAll().stream()
                    .filter(a -> a.getQuestionId().equals(q.getId())).count();
            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("should return 409 when session is already COMPLETED")
        void shouldReturn409_ifSessionCompleted() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            // Complete the session first
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk());

            // Now attempt to submit answers — must fail
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Late Q", 1, false);

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "Too late")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when session belongs to another workspace")
        void shouldReturn403_wrongWorkspace() throws Exception {
            AuthContext other = setupSecondUser();
            JpaBriefingSession otherBriefing = createBriefingSession(other.workspaceId());
            JpaProposal otherProposal = createDraftProposal(other.workspaceId(), otherBriefing.getId());
            String otherSessionId = createSessionViaApi(other, otherProposal.getId());

            JpaServiceContextProfile profile = createServiceContextProfile(other.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", otherSessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "Unauthorized answer")))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ POST /briefing-sessions/{id}/complete ============

    @Nested
    @DisplayName("POST /api/v1/briefing-sessions/{id}/complete")
    class CompleteBriefingSession {

        @Test
        @DisplayName("should return 200 with status COMPLETED and score=100 when no profile exists")
        void shouldReturn100_whenNoProfile() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completenessScore").value(100))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should calculate score=60 when 3 of 5 required questions answered")
        void shouldCalculateScore_3of5Required() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            // Create profile with 5 required questions
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q1 = createServiceContextQuestion(profile.getId(), "Q1", 1, true);
            JpaServiceContextQuestion q2 = createServiceContextQuestion(profile.getId(), "Q2", 2, true);
            JpaServiceContextQuestion q3 = createServiceContextQuestion(profile.getId(), "Q3", 3, true);
            JpaServiceContextQuestion q4 = createServiceContextQuestion(profile.getId(), "Q4", 4, true);
            JpaServiceContextQuestion q5 = createServiceContextQuestion(profile.getId(), "Q5", 5, true);

            // Answer only 3 of the 5 required questions
            List<UUID> answeredIds = List.of(q1.getId(), q2.getId(), q3.getId());
            String body = answersJson(answeredIds);

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                            .header("Authorization", auth.authorizationHeader())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completenessScore").value(60))
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("should include warning message in response when score < 80")
        void shouldReturnWarningMessage_whenScoreBelow80() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            // Create profile with 5 required questions — answer none → score 0
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            createServiceContextQuestion(profile.getId(), "Q1", 1, true);
            createServiceContextQuestion(profile.getId(), "Q2", 2, true);
            createServiceContextQuestion(profile.getId(), "Q3", 3, true);
            createServiceContextQuestion(profile.getId(), "Q4", 4, true);
            createServiceContextQuestion(profile.getId(), "Q5", 5, true);

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("low score")));
        }

        @Test
        @DisplayName("should include success message in response when score >= 80")
        void shouldReturnSuccessMessage_whenScoreAbove80() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            // No profile → score = 100
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("successfully")));
        }

        @Test
        @DisplayName("should return 409 when session is already COMPLETED")
        void shouldReturn409_whenAlreadyCompleted() throws Exception {
            String sessionId = createSessionViaApi(auth, proposal.getId());

            // First completion
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk());

            // Second completion attempt — must be rejected
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("should return 403 when session belongs to another workspace")
        void shouldReturn403_wrongWorkspace() throws Exception {
            AuthContext other = setupSecondUser();
            JpaBriefingSession otherBriefing = createBriefingSession(other.workspaceId());
            JpaProposal otherProposal = createDraftProposal(other.workspaceId(), otherBriefing.getId());
            String otherSessionId = createSessionViaApi(other, otherProposal.getId());

            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", otherSessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ GET /briefing-sessions/token/{token} ============

    @Nested
    @DisplayName("GET /api/v1/briefing-sessions/token/{token}")
    class GetByPublicToken {

        @Test
        @DisplayName("should return session by valid public token (authenticated)")
        void shouldReturnByToken() throws Exception {
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
            String token = body.get("publicToken").asText();

            mockMvc.perform(get("/api/v1/briefing-sessions/token/{token}", token)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.publicToken").value(token));
        }

        @Test
        @DisplayName("should return 404 for unknown token")
        void shouldReturn404_forUnknownToken() throws Exception {
            mockMvc.perform(get("/api/v1/briefing-sessions/token/unknown-token-does-not-exist")
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 403 when session belongs to another workspace")
        void shouldReturn403_wrongWorkspace() throws Exception {
            // Create session in workspace A
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
            String tokenInWorkspaceA = body.get("publicToken").asText();

            // Create user in workspace B
            AuthContext other = setupSecondUser();

            // Try to access session from workspace A using workspace B credentials
            mockMvc.perform(get("/api/v1/briefing-sessions/token/{token}", tokenInWorkspaceA)
                            .header("Authorization", other.authorizationHeader()))
                    .andExpect(status().isForbidden());
        }
    }

    // ============ Public endpoints (no auth) ============

    @Nested
    @DisplayName("Public endpoints — /public/briefings/{token}")
    class PublicEndpoints {

        @Test
        @DisplayName("GET /public/briefings/{token}/questions — returns questions without auth")
        void shouldReturnQuestions_withoutAuth() throws Exception {
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            String token = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()).get("publicToken").asText();

            // Create a profile so questions are returned
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            createServiceContextQuestion(profile.getId(), "Public Q1", 1, true);

            mockMvc.perform(get("/public/briefings/{token}/questions", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("GET /public/briefings/{token}/questions — returns 404 for invalid token")
        void shouldReturn404_forInvalidToken() throws Exception {
            mockMvc.perform(get("/public/briefings/invalid-non-existent-token/questions"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /public/briefings/{token}/batch-answers — submits answers without auth")
        void shouldSubmitBatchAnswers_withoutAuth() throws Exception {
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            String token = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()).get("publicToken").asText();

            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Public Q", 1, true);

            mockMvc.perform(post("/public/briefings/{token}/batch-answers", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "Client answer")))
                    .andExpect(status().isNoContent());

            assertThat(answerRepo.findAll()).anyMatch(a -> a.getQuestionId().equals(q.getId()));
        }

        @Test
        @DisplayName("POST /public/briefings/{token}/batch-answers — returns 404 for invalid token")
        void shouldReturn404_batchAnswers_invalidToken() throws Exception {
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);

            mockMvc.perform(post("/public/briefings/invalid-token-xyz/batch-answers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "some answer")))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("POST /public/briefings/{token}/batch-answers — returns 409 when session COMPLETED")
        void shouldReturn409_batchAnswers_sessionCompleted() throws Exception {
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
            String sessionId = created.get("id").asText();
            String token = created.get("publicToken").asText();

            // Complete the session via authenticated endpoint
            mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                            .header("Authorization", auth.authorizationHeader()))
                    .andExpect(status().isOk());

            // Try to submit answers via public token on a COMPLETED session
            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);

            mockMvc.perform(post("/public/briefings/{token}/batch-answers", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(singleAnswerJson(q.getId(), "Too late")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("POST /public/briefings/{token}/batch-answers — idempotent, duplicate skipped")
        void shouldBeIdempotent_batchAnswers() throws Exception {
            MvcResult createResult = mockMvc.perform(
                            post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                    .header("Authorization", auth.authorizationHeader()))
                    .andReturn();

            String token = objectMapper.readTree(
                    createResult.getResponse().getContentAsString()).get("publicToken").asText();

            JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
            JpaServiceContextQuestion q = createServiceContextQuestion(profile.getId(), "Q1", 1, true);
            String body = singleAnswerJson(q.getId(), "Client answer");

            // Submit twice — both must return 204
            mockMvc.perform(post("/public/briefings/{token}/batch-answers", token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/public/briefings/{token}/batch-answers", token)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isNoContent());

            // Only one row
            long count = answerRepo.findAll().stream()
                    .filter(a -> a.getQuestionId().equals(q.getId())).count();
            assertThat(count).isEqualTo(1);
        }
    }

    // ============ Full end-to-end flow ============

    @Test
    @DisplayName("full briefing flow: create → questions → submit answers → complete (score 100%)")
    void fullBriefingFlow_score100() throws Exception {
        // Step 1: Create session
        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/proposals/{id}/briefing-sessions", proposal.getId())
                                .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String sessionId = created.get("id").asText();
        assertThat(created.get("status").asText()).isEqualTo("IN_PROGRESS");

        // Step 2: Create profile with 2 required questions
        JpaServiceContextProfile profile = createServiceContextProfile(auth.workspaceId(), "SOCIAL_MEDIA");
        JpaServiceContextQuestion q1 = createServiceContextQuestion(profile.getId(), "What is your goal?", 1, true);
        JpaServiceContextQuestion q2 = createServiceContextQuestion(profile.getId(), "Target audience?", 2, true);

        // Step 3: Verify questions endpoint returns both
        mockMvc.perform(get("/api/v1/briefing-sessions/{id}/questions", sessionId)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Step 4: Submit answers for both required questions
        String answersBody = answersJson(List.of(q1.getId(), q2.getId()));
        mockMvc.perform(post("/api/v1/briefing-sessions/{id}/answers", sessionId)
                        .header("Authorization", auth.authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answersBody))
                .andExpect(status().isNoContent());

        // Step 5: Complete — should be 100% (all required answered)
        mockMvc.perform(post("/api/v1/briefing-sessions/{id}/complete", sessionId)
                        .header("Authorization", auth.authorizationHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completenessScore").value(100))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // ============ Helpers ============

    /**
     * Create a briefing session via the API and return its ID string.
     */
    private String createSessionViaApi(AuthContext authCtx, UUID pid) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/api/v1/proposals/{id}/briefing-sessions", pid)
                                .header("Authorization", authCtx.authorizationHeader()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();
    }

    /**
     * Create a second independent user+workspace for cross-workspace isolation tests.
     */
    private AuthContext setupSecondUser() {
        var user = createActiveUser(UUID.randomUUID(), "other-" + UUID.randomUUID() + "@test.com");
        var workspace = createWorkspace(user.getId(), "Other Workspace " + UUID.randomUUID());
        addOwnerMember(workspace.getId(), user.getId());
        String token = bearerToken(user.getId(), user.getEmail(), workspace.getId(), "OWNER");
        return new AuthContext(user.getId(), workspace.getId(), token);
    }

    private JpaServiceContextProfile createServiceContextProfile(UUID wkspId, String serviceType) {
        JpaServiceContextProfile profile = new JpaServiceContextProfile(
                UUID.randomUUID(), wkspId, serviceType,
                "Test Profile " + serviceType + " " + UUID.randomUUID(),
                null, null, null, null, null,
                true, Instant.now(), Instant.now()
        );
        return profileRepo.save(profile);
    }

    private JpaServiceContextQuestion createServiceContextQuestion(
            UUID profileId, String text, int order, boolean required) {
        JpaServiceContextQuestion q = new JpaServiceContextQuestion(
                UUID.randomUUID(), profileId, text, "OPEN_ENDED", order, required, Instant.now()
        );
        return questionRepo.save(q);
    }
}
