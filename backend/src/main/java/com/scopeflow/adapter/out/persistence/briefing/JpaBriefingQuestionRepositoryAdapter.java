package com.scopeflow.adapter.out.persistence.briefing;

import com.scopeflow.core.domain.briefing.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of BriefingQuestionRepository domain port.
 * Converts between domain objects and JPA entities.
 */
@Component
public class JpaBriefingQuestionRepositoryAdapter implements BriefingQuestionRepository {

    private final JpaBriefingQuestionSpringRepository repo;

    public JpaBriefingQuestionRepositoryAdapter(JpaBriefingQuestionSpringRepository repo) {
        this.repo = repo;
    }

    @Override
    public Optional<BriefingQuestion> findBySessionAndStep(BriefingSessionId sessionId, int step) {
        return repo.findBySessionAndStep(sessionId.value(), step)
                .map(this::toDomain);
    }

    @Override
    public List<BriefingQuestion> findBySession(BriefingSessionId sessionId) {
        return repo.findBySessionOrderByStepAsc(sessionId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void save(BriefingQuestion question) {
        JpaBriefingQuestion jpaEntity = toJpaEntity(question);
        repo.save(jpaEntity);
    }

    @Override
    public long countByServiceType(ServiceType serviceType) {
        // This would require a query by service_type in future
        // For now, return 0 (not used in MVP)
        return 0;
    }

    // ============ Domain → JPA Mapping ============

    private JpaBriefingQuestion toJpaEntity(BriefingQuestion question) {
        return new JpaBriefingQuestion(
                question.getId().value(),
                question.getSessionId().value(),
                question.getText(),
                question.getStep(),
                question.getQuestionType(),
                "v1", // aiPromptVersion (default)
                true, // required (default)
                false, // followUpGenerated (would need to check if this is a follow-up)
                question.getCreatedAt()
        );
    }

    // ============ JPA → Domain Mapping ============

    private BriefingQuestion toDomain(JpaBriefingQuestion jpa) {
        return new BriefingQuestion(
                new QuestionId(jpa.getId()),
                new BriefingSessionId(jpa.getBriefingSessionId()),
                jpa.getQuestionText(),
                jpa.getStep(),
                jpa.getQuestionType(),
                jpa.getCreatedAt()
        );
    }
}
