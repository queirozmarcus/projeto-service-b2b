package com.scopeflow.core.domain.briefing;

import java.util.List;

/**
 * GapAnalysis value object: represents briefing completion analysis at any score level.
 *
 * Unlike CompletionScore (which enforces score >= 80), GapAnalysis can represent
 * any state from 0% to 100%, including incomplete briefings.
 *
 * Used by BriefingService.detectGaps() to always return a non-null result.
 * If score >= 80, the briefing is eligible for completion.
 */
public record GapAnalysis(int score, List<String> gaps) {

    public GapAnalysis {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be 0-100, got " + score);
        }
        gaps = gaps == null ? List.of() : List.copyOf(gaps);
    }

    /**
     * Whether the briefing meets the minimum completion threshold.
     */
    public boolean isEligibleForCompletion() {
        return score >= 80;
    }

    /**
     * Whether there are identified gaps.
     */
    public boolean hasGaps() {
        return !gaps.isEmpty();
    }
}
