package com.scopeflow.adapter.in.web.briefing.fixtures;

import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Test fixtures for briefing domain objects.
 * Provides factory methods for creating test data.
 */
public class BriefingTestFixtures {

    public static WorkspaceId DEFAULT_WORKSPACE_ID = new WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    public static ClientId DEFAULT_CLIENT_ID = new ClientId(UUID.randomUUID());
    public static ServiceType DEFAULT_SERVICE_TYPE = ServiceType.SOCIAL_MEDIA;

    /**
     * Create a BriefingInProgress (new session) for testing.
     */
    public static BriefingInProgress createTestBriefingInProgress() {
        return createTestBriefingInProgress(DEFAULT_WORKSPACE_ID, DEFAULT_CLIENT_ID, DEFAULT_SERVICE_TYPE);
    }

    public static BriefingInProgress createTestBriefingInProgress(
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType
    ) {
        return BriefingSession.startNew(workspaceId, clientId, serviceType);
    }

    /**
     * Create a BriefingCompleted for testing.
     */
    public static BriefingCompleted createTestBriefingCompleted() {
        var inProgress = createTestBriefingInProgress();
        var score = new CompletionScore(95, List.of());
        return inProgress.completeBriefing(score);
    }

    /**
     * Create a BriefingAbandoned for testing.
     */
    public static BriefingAbandoned createTestBriefingAbandoned() {
        var inProgress = createTestBriefingInProgress();
        return inProgress.abandon();
    }

    /**
     * Create a BriefingQuestion for testing.
     */
    public static BriefingQuestion createTestQuestion(BriefingSessionId sessionId, int step) {
        return new BriefingQuestion(
                QuestionId.generate(),
                sessionId,
                "Test question " + step + "?",
                step,
                "OPEN",
                Instant.now()
        );
    }

    /**
     * Create a BriefingAnswer (AnsweredDirect) for testing.
     */
    public static AnsweredDirect createTestAnswer(BriefingSessionId sessionId, QuestionId questionId) {
        return new AnsweredDirect(
                AnswerId.generate(),
                sessionId,
                questionId,
                new AnswerText("Test answer text"),
                Instant.now(),
                85
        );
    }

    /**
     * Create a CompletionScore for testing.
     */
    public static CompletionScore createTestCompletionScore(int score) {
        return new CompletionScore(score, score < 100 ? List.of("Need more details") : List.of());
    }
}
