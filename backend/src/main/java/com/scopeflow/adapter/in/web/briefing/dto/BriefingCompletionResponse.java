package com.scopeflow.adapter.in.web.briefing.dto;

/**
 * Response DTO after completing a briefing session.
 * Reports the calculated completeness score.
 */
public record BriefingCompletionResponse(
        int completenessScore,
        String status,
        String message
) {}
