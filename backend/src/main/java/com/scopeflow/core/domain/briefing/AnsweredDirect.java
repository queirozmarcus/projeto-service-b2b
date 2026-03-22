package com.scopeflow.core.domain.briefing;

import java.time.Instant;

/**
 * Answer without auto-generated follow-up: direct response to question with quality score.
 */
public final class AnsweredDirect extends BriefingAnswer {
    private final int qualityScore; // 0-100

    public AnsweredDirect(
            AnswerId id,
            BriefingSessionId sessionId,
            QuestionId questionId,
            AnswerText text,
            Instant answeredAt,
            int qualityScore
    ) {
        super(id, sessionId, questionId, text, answeredAt);
        if (qualityScore < 0 || qualityScore > 100) {
            throw new IllegalArgumentException("Quality score must be 0-100, got " + qualityScore);
        }
        this.qualityScore = qualityScore;
    }

    @Override
    public String answerType() {
        return "DIRECT";
    }

    @Override
    public int getQualityScore() {
        return qualityScore;
    }

    @Override
    public boolean hasFollowup() {
        return false;
    }
}
