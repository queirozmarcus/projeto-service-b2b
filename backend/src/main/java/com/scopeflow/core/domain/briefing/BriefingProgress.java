package com.scopeflow.core.domain.briefing;

/**
 * BriefingProgress value object: tracks current step, total steps, and completion percentage.
 * Immutable, uses record with compact constructor validation and automatic percentage calculation.
 */
public record BriefingProgress(int currentStep, int totalSteps, int completionPercentage) {
    public BriefingProgress {
        if (currentStep < 0 || currentStep > totalSteps) {
            throw new IllegalArgumentException(
                    String.format("Current step (%d) must be >= 0 and <= totalSteps (%d)", currentStep, totalSteps)
            );
        }
        int calculatedPercentage = totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps;
        if (completionPercentage != calculatedPercentage) {
            throw new IllegalArgumentException(
                    String.format("Completion percentage mismatch: expected %d, got %d", calculatedPercentage, completionPercentage)
            );
        }
    }

    /**
     * Create progress with automatic percentage calculation.
     */
    public static BriefingProgress of(int currentStep, int totalSteps) {
        int percentage = totalSteps == 0 ? 0 : (currentStep * 100) / totalSteps;
        return new BriefingProgress(currentStep, totalSteps, percentage);
    }

    /**
     * Advance to next step.
     */
    public BriefingProgress nextStep() {
        if (currentStep >= totalSteps) {
            throw new IllegalStateException("Cannot advance beyond total steps");
        }
        return BriefingProgress.of(currentStep + 1, totalSteps);
    }
}
