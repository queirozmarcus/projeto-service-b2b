package com.scopeflow.adapter.in.web.briefing.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.briefing.mapper.BriefingMapper;
import com.scopeflow.adapter.out.persistence.briefing.*;
import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all Briefing integration tests.
 *
 * Provides:
 * - Testcontainers PostgreSQL 16-Alpine
 * - Spring Boot context (full application)
 * - MockMvc for HTTP requests
 * - ObjectMapper for JSON serialization
 * - Repository access for setup/verification
 * - Helper methods for test data creation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
abstract class BriefingIntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected BriefingMapper mapper;

    @Autowired
    protected JpaBriefingSessionSpringRepository sessionRepository;

    @Autowired
    protected JpaBriefingQuestionSpringRepository questionRepository;

    @Autowired
    protected JpaBriefingAnswerSpringRepository answerRepository;

    @Autowired
    protected BriefingService briefingService;

    // Test constants
    protected static final UUID WORKSPACE_ID_A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    protected static final UUID WORKSPACE_ID_B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    protected static final UUID CLIENT_ID = UUID.randomUUID();
    protected static final ServiceType DEFAULT_SERVICE = ServiceType.SOCIAL_MEDIA;

    @BeforeEach
    void cleanDatabase() {
        // Clean in correct order to avoid FK violations
        answerRepository.deleteAll();
        questionRepository.deleteAll();
        sessionRepository.deleteAll();
    }

    /**
     * Create and persist a test briefing session.
     */
    protected BriefingSession createTestBriefing(UUID workspaceId, UUID clientId, ServiceType serviceType) {
        var session = BriefingSession.startNew(
                new WorkspaceId(workspaceId),
                new ClientId(clientId),
                serviceType
        );

        var entity = JpaBriefingSessionEntity.fromDomain(session);
        var saved = sessionRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * Create and persist a completed briefing.
     */
    protected BriefingSession createCompletedBriefing(UUID workspaceId, UUID clientId) {
        var session = createTestBriefing(workspaceId, clientId, DEFAULT_SERVICE);
        var completed = ((BriefingInProgress) session).completeBriefing(
                new CompletionScore(95, java.util.List.of())
        );

        var entity = JpaBriefingSessionEntity.fromDomain(completed);
        var saved = sessionRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * Create and persist a test question.
     */
    protected BriefingQuestion createTestQuestion(BriefingSessionId sessionId, int step, String text) {
        var question = new BriefingQuestion(
                QuestionId.generate(),
                sessionId,
                text,
                step,
                "OPEN",
                Instant.now()
        );

        var entity = JpaBriefingQuestionEntity.fromDomain(question);
        var saved = questionRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * Create and persist a test answer.
     */
    protected BriefingAnswer createTestAnswer(BriefingSessionId sessionId, QuestionId questionId, String answerText) {
        var answer = new AnsweredDirect(
                AnswerId.generate(),
                sessionId,
                questionId,
                new AnswerText(answerText),
                Instant.now(),
                85
        );

        var entity = JpaBriefingAnswerEntity.fromDomain(answer);
        var saved = answerRepository.save(entity);
        return saved.toDomain();
    }

    /**
     * Generate a valid JWT token for testing (mocked).
     *
     * In real tests, this should be replaced with actual JWT generation
     * using JwtService or similar.
     */
    protected String generateTestJwtToken(UUID workspaceId, String username) {
        // TODO: Replace with actual JWT generation when auth module is complete
        // For now, return a mock token that SecurityUtil can extract workspaceId from
        return "Bearer mock-jwt-token-" + workspaceId + "-" + username;
    }
}
