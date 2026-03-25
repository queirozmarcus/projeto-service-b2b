package com.scopeflow.adapter.in.web.briefing.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a BriefingSession linked to a Proposal.
 */
public record BriefingSessionResponse(
        UUID id,
        UUID proposalId,
        String status,
        String publicToken,
        Integer completenessScore,
        Instant createdAt,
        Instant updatedAt
) {}
