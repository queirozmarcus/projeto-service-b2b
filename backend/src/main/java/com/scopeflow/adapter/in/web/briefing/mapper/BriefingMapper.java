package com.scopeflow.adapter.in.web.briefing.mapper;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.UUID;

/**
 * Mapper interface for converting between domain objects and DTOs.
 *
 * Implementation will be provided by backend-dev (Step 5).
 * This interface defines the contract for mapping operations.
 */
public interface BriefingMapper {

    // ============ Domain → DTO ============

    /**
     * Map BriefingSession (any subtype) to BriefingResponse.
     */
    BriefingResponse toResponse(BriefingSession session);

    /**
     * Map BriefingSession with details to BriefingDetailResponse.
     */
    BriefingDetailResponse toDetailResponse(
            BriefingSession session,
            List<BriefingQuestion> questions,
            List<BriefingAnswer> answers
    );

    /**
     * Map BriefingSession to PublicBriefingResponse (no sensitive data).
     */
    PublicBriefingResponse toPublicResponse(BriefingSession session);

    /**
     * Map BriefingProgress to ProgressResponse.
     */
    ProgressResponse toProgressResponse(BriefingProgress progress, List<String> gaps);

    /**
     * Map CompletionScore to ProgressResponse.
     */
    ProgressResponse toProgressResponse(CompletionScore score);

    /**
     * Map GapAnalysis to ProgressResponse (used by detectGaps — score may be < 80).
     */
    ProgressResponse toProgressResponse(GapAnalysis analysis);

    /**
     * Map BriefingQuestion to QuestionResponse.
     */
    QuestionResponse toQuestionResponse(BriefingQuestion question, boolean followUpGenerated);

    /**
     * Map BriefingAnswer to AnswerResponse.
     */
    AnswerResponse toAnswerResponse(BriefingAnswer answer);

    // ============ DTO → Domain ============

    /**
     * Convert CreateBriefingRequest to domain value objects.
     */
    ClientId toClientId(UUID clientId);

    /**
     * Convert ServiceType from string to domain enum.
     */
    ServiceType toServiceType(String serviceType);

    /**
     * Convert SubmitAnswerRequest to domain value objects.
     */
    QuestionId toQuestionId(UUID questionId);

    /**
     * Convert answer text to domain value object.
     */
    AnswerText toAnswerText(String answerText);

    /**
     * Convert CompleteBriefingRequest to CompletionScore.
     */
    CompletionScore toCompletionScore(CompleteBriefingRequest request);

    /**
     * Convert PublicToken from UUID to domain value object.
     */
    PublicToken toPublicToken(UUID publicToken);

    /**
     * Convert BriefingSessionId from UUID to domain value object.
     */
    BriefingSessionId toBriefingSessionId(UUID sessionId);

    /**
     * Convert WorkspaceId from UUID to domain value object.
     */
    WorkspaceId toWorkspaceId(UUID workspaceId);

    // ============ Pagination ============

    /**
     * Create a PageResponse from Spring Data Page content.
     */
    <T> PageResponse<T> toPageResponse(
            List<T> content,
            long totalElements,
            int totalPages,
            int size,
            int number,
            boolean first,
            boolean last
    );
}
