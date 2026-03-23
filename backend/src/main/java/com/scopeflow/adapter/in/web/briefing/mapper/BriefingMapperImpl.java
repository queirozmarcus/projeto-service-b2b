package com.scopeflow.adapter.in.web.briefing.mapper;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of BriefingMapper.
 *
 * Converts between domain objects and REST DTOs.
 * Zero domain knowledge — pure translation.
 */
@Component
public class BriefingMapperImpl implements BriefingMapper {

    @Override
    public BriefingResponse toResponse(BriefingSession session) {
        if (session == null) {
            return null;
        }

        Integer completionScore = null;
        if (session instanceof BriefingCompleted completed) {
            completionScore = completed.getCompletionScore().score();
        }

        return new BriefingResponse(
                session.getId().value(),
                session.getWorkspaceId().value(),
                session.getClientId().value(),
                session.getServiceType().name(),
                session.status(),
                session.getPublicToken().value(),
                completionScore,
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    @Override
    public BriefingDetailResponse toDetailResponse(
            BriefingSession session,
            List<BriefingQuestion> questions,
            List<BriefingAnswer> answers
    ) {
        if (session == null) {
            return null;
        }

        BriefingResponse briefingResponse = toResponse(session);
        ProgressResponse progress = toProgressResponse(session.completionScore());

        List<QuestionResponse> questionResponses = questions.stream()
                .map(q -> toQuestionResponse(q, false)) // TODO: detect if followup generated
                .toList();

        List<AnswerResponse> answerResponses = answers.stream()
                .map(this::toAnswerResponse)
                .toList();

        return new BriefingDetailResponse(
                briefingResponse,
                progress,
                questionResponses,
                answerResponses
        );
    }

    @Override
    public PublicBriefingResponse toPublicResponse(BriefingSession session) {
        if (session == null) {
            return null;
        }

        // No sensitive fields: no workspaceId, no clientId
        CompletionScore score = null;
        if (session instanceof BriefingCompleted completed) {
            score = completed.getCompletionScore();
        }
        ProgressResponse progress = toProgressResponse(score);

        return new PublicBriefingResponse(
                session.getId().value(),
                session.getServiceType().name(),
                session.status(),
                progress,
                session.getCreatedAt()
        );
    }

    @Override
    public ProgressResponse toProgressResponse(BriefingProgress progress, List<String> gaps) {
        if (progress == null) {
            return new ProgressResponse(0, 0, 0, List.of());
        }

        return new ProgressResponse(
                progress.currentStep(),
                progress.totalSteps(),
                progress.completionPercentage(),
                gaps
        );
    }

    @Override
    public ProgressResponse toProgressResponse(CompletionScore score) {
        if (score == null) {
            return new ProgressResponse(0, 0, 0, List.of());
        }

        return new ProgressResponse(
                0, // TODO: calculate from answers count
                0, // TODO: calculate from questions count
                score.score(),
                score.gapsIdentified()
        );
    }

    @Override
    public QuestionResponse toQuestionResponse(BriefingQuestion question, boolean followUpGenerated) {
        if (question == null) {
            return null;
        }

        return new QuestionResponse(
                question.getId().value(),
                question.getText(),
                question.getStep(),
                question.getQuestionType(),
                true, // TODO: Add required field to domain model (for now, all questions are required)
                followUpGenerated
        );
    }

    @Override
    public AnswerResponse toAnswerResponse(BriefingAnswer answer) {
        if (answer == null) {
            return null;
        }

        Integer qualityScore = null;
        if (answer instanceof AnsweredDirect direct) {
            qualityScore = direct.getQualityScore();
        } else if (answer instanceof AnsweredWithFollowup followup) {
            qualityScore = followup.getConfidenceScore();
        }

        return new AnswerResponse(
                answer.getId().value(),
                answer.getQuestionId().value(),
                answer.getText().value(),
                qualityScore,
                answer.getAnsweredAt()
        );
    }

    // ============ DTO → Domain ============

    @Override
    public ClientId toClientId(UUID clientId) {
        return new ClientId(clientId);
    }

    @Override
    public ServiceType toServiceType(String serviceType) {
        return ServiceType.valueOf(serviceType);
    }

    @Override
    public QuestionId toQuestionId(UUID questionId) {
        return new QuestionId(questionId);
    }

    @Override
    public AnswerText toAnswerText(String answerText) {
        return new AnswerText(answerText);
    }

    @Override
    public CompletionScore toCompletionScore(CompleteBriefingRequest request) {
        return new CompletionScore(
                request.completionScore(),
                request.gapsIdentified() != null ? request.gapsIdentified() : List.of()
        );
    }

    @Override
    public PublicToken toPublicToken(UUID publicToken) {
        return new PublicToken(publicToken);
    }

    @Override
    public BriefingSessionId toBriefingSessionId(UUID sessionId) {
        return new BriefingSessionId(sessionId);
    }

    @Override
    public WorkspaceId toWorkspaceId(UUID workspaceId) {
        return new WorkspaceId(workspaceId);
    }

    // ============ Pagination ============

    @Override
    public <T> PageResponse<T> toPageResponse(
            List<T> content,
            long totalElements,
            int totalPages,
            int size,
            int number,
            boolean first,
            boolean last
    ) {
        return new PageResponse<>(
                content,
                totalElements,
                totalPages,
                size,
                number,
                first,
                last
        );
    }
}
