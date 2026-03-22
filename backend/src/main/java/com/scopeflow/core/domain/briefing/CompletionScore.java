package com.scopeflow.core.domain.briefing;

import java.util.List;

/**
 * CompletionScore value object: represents briefing completion score with identified gaps.
 * Must be >= 80 to be valid for completion.
 * Immutable, uses record with compact constructor validation.
 */
public record CompletionScore(int score, List<String> gapsIdentified) {
    public CompletionScore {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Score must be 0-100, got " + score);
        }
        if (score < 80) {
            throw new IllegalArgumentException(
                    String.format("Completion score must be >= 80 to complete, got %d", score)
            );
        }
        if (gapsIdentified == null) {
            gapsIdentified = List.of();
        } else {
            gapsIdentified = List.copyOf(gapsIdentified); // Make immutable
        }
    }

    /**
     * Check if completion is optimal (no gaps).
     */
    public boolean isPerfect() {
        return score >= 95 && gapsIdentified.isEmpty();
    }

    /**
     * Check if completion is acceptable but has gaps.
     */
    public boolean hasGaps() {
        return !gapsIdentified.isEmpty();
    }
}
