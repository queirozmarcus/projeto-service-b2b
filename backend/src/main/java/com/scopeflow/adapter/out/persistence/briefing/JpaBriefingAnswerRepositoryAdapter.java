package com.scopeflow.adapter.out.persistence.briefing;

import com.scopeflow.core.domain.briefing.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA implementation of BriefingAnswerRepository domain port.
 * Converts between domain objects and JPA entities.
 */
@Component
@Transactional(readOnly = true)
public class JpaBriefingAnswerRepositoryAdapter implements BriefingAnswerRepository {

    private final JpaBriefingAnswerSpringRepository repo;

    public JpaBriefingAnswerRepositoryAdapter(JpaBriefingAnswerSpringRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional
    public void save(BriefingAnswer answer) {
        JpaBriefingAnswer jpaEntity = toJpaEntity(answer);
        repo.save(jpaEntity);
    }

    @Override
    public List<BriefingAnswer> findBySession(BriefingSessionId sessionId) {
        return repo.findBySessionOrderByCreatedAt(sessionId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countFollowupsByQuestion(QuestionId questionId) {
        return repo.countFollowupsByQuestion(questionId.value());
    }

    @Override
    public boolean existsBySessionAndQuestion(BriefingSessionId sessionId, QuestionId questionId) {
        // Simple check via findBySession + filter (could be optimized with dedicated query)
        return repo.findBySessionOrderByCreatedAt(sessionId.value())
                .stream()
                .anyMatch(answer -> answer.getQuestionId().equals(questionId.value()));
    }

    // ============ Domain → JPA Mapping ============

    private JpaBriefingAnswer toJpaEntity(BriefingAnswer answer) {
        return new JpaBriefingAnswer(
                answer.getId().value(),
                answer.getSessionId().value(),
                answer.getQuestionId().value(),
                answer.getText().value(),
                null, // answerJson (optional, not used in MVP)
                answer.getQualityScore(),
                null, // aiAnalysis (optional, not used in MVP)
                answer.getAnsweredAt()
        );
    }

    // ============ JPA → Domain Mapping ============

    private BriefingAnswer toDomain(JpaBriefingAnswer jpa) {
        // For MVP, always map to AnsweredDirect (no follow-up logic implemented yet)
        // In full implementation, check if follow-up exists and return AnsweredWithFollowup
        return new AnsweredDirect(
                new AnswerId(jpa.getId()),
                new BriefingSessionId(jpa.getBriefingSessionId()),
                new QuestionId(jpa.getQuestionId()),
                new AnswerText(jpa.getAnswerText()),
                jpa.getCreatedAt(),
                jpa.getQualityScore() != null ? jpa.getQualityScore() : 0
        );
    }
}
