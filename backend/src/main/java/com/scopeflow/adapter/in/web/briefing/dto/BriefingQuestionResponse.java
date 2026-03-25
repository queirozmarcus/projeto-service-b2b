package com.scopeflow.adapter.in.web.briefing.dto;

import java.util.UUID;

/**
 * Response DTO for a question in the briefing discovery flow.
 * Questions come from the ServiceContextProfile templates.
 */
public record BriefingQuestionResponse(
        UUID questionId,
        String questionText,
        String type,
        int orderIndex,
        boolean required
) {}
