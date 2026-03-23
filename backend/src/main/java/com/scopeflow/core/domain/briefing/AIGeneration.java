package com.scopeflow.core.domain.briefing;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * AIGeneration value object: audit trail for all AI-generated outputs.
 * Tracks type, inputs, outputs, prompt version, latency, and cost.
 * Immutable for auditability.
 */
public record AIGeneration(
        GenerationType type,
        String inputJson,
        String outputJson,
        String promptVersion,
        long latencyMs,
        BigDecimal costUsd
) {
    public AIGeneration {
        Objects.requireNonNull(type, "GenerationType cannot be null");
        Objects.requireNonNull(inputJson, "InputJson cannot be null");
        Objects.requireNonNull(outputJson, "OutputJson cannot be null");
        Objects.requireNonNull(promptVersion, "PromptVersion cannot be null");
        Objects.requireNonNull(costUsd, "CostUsd cannot be null");

        if (latencyMs < 0) {
            throw new IllegalArgumentException("LatencyMs must be >= 0, got " + latencyMs);
        }
        if (costUsd.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("CostUsd must be >= 0, got " + costUsd);
        }
    }

    /**
     * Check if generation was fast (< 1s).
     */
    public boolean wasFast() {
        return latencyMs < 1000;
    }

    /**
     * Check if generation was slow (> 5s).
     */
    public boolean wasSlow() {
        return latencyMs > 5000;
    }
}
