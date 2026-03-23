package com.scopeflow.core.domain.briefing;

import java.time.Instant;
import java.util.Objects;

/**
 * Answer with auto-generated follow-up: AI detected gaps and generated follow-up question.
 */
public final class AnsweredWithFollowup extends BriefingAnswer {
    private final BriefingQuestion generatedFollowup;
    private final int confidenceScore; // 0-100

    public AnsweredWithFollowup(
            AnswerId id,
            BriefingSessionId sessionId,
            QuestionId questionId,
            AnswerText text,
            Instant answeredAt,
            BriefingQuestion generatedFollowup,
            int confidenceScore
    ) {
        super(id, sessionId, questionId, text, answeredAt);
        this.generatedFollowup = Objects.requireNonNull(generatedFollowup, "GeneratedFollowup cannot be null");
        if (confidenceScore < 0 || confidenceScore > 100) {
            throw new IllegalArgumentException("Confidence score must be 0-100, got " + confidenceScore);
        }
        this.confidenceScore = confidenceScore;
    }

    @Override
    public String answerType() {
        return "WITH_FOLLOWUP";
    }

    @Override
    public int getQualityScore() {
        return confidenceScore; // Use confidence as quality for consistency
    }

    @Override
    public boolean hasFollowup() {
        return true;
    }

    public BriefingQuestion getGeneratedFollowup() {
        return generatedFollowup;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }
}
