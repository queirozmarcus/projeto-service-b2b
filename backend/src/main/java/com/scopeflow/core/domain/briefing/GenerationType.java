package com.scopeflow.core.domain.briefing;

/**
 * GenerationType enum: categorizes AI generation outputs.
 */
public enum GenerationType {
    FOLLOW_UP_QUESTION("Follow-up question generated based on gap detection"),
    GAP_ANALYSIS("Analysis identifying gaps in answers"),
    COMPLETION_SUMMARY("Summary and completeness score"),
    BRIEFING_CONSOLIDATION("Consolidated briefing from all answers");

    private final String description;

    GenerationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
