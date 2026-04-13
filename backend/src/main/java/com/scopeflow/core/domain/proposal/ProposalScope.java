package com.scopeflow.core.domain.proposal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * ProposalScope value object: immutable snapshot of what is included/excluded.
 *
 * Stored as JSONB in proposal_versions table.
 */
public record ProposalScope(
        List<Deliverable> deliverables,
        List<String> exclusions,
        List<String> assumptions,
        Price price,
        Timeline timeline
) {
    public ProposalScope {
        Objects.requireNonNull(deliverables, "Deliverables cannot be null");
        Objects.requireNonNull(exclusions, "Exclusions cannot be null");
        Objects.requireNonNull(assumptions, "Assumptions cannot be null");
        deliverables = List.copyOf(deliverables);
        exclusions = List.copyOf(exclusions);
        assumptions = List.copyOf(assumptions);
    }

    public record Deliverable(
            String name,
            String description,
            String acceptanceCriteria
    ) {
        public Deliverable {
            Objects.requireNonNull(name, "Deliverable name cannot be null");
        }
    }

    public record Price(
            BigDecimal amount,
            String currency,
            String breakdown
    ) {
        public Price {
            Objects.requireNonNull(amount, "Price amount cannot be null");
            Objects.requireNonNull(currency, "Currency cannot be null");
        }
    }

    public record Timeline(
            LocalDate startDate,
            LocalDate endDate,
            List<Milestone> milestones
    ) {
        public Timeline {
            milestones = milestones != null ? List.copyOf(milestones) : List.of();
        }
    }

    public record Milestone(
            String name,
            LocalDate dueDate,
            String description
    ) {}
}
