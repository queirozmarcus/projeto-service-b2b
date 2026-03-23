package com.scopeflow.adapter.out.persistence.briefing;

import com.scopeflow.core.domain.briefing.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * JPA implementation of AIGenerationRepository domain port.
 * Converts between domain objects and JPA entities.
 */
@Component
public class JpaAIGenerationRepositoryAdapter implements AIGenerationRepository {

    private final JpaAIGenerationSpringRepository repo;

    public JpaAIGenerationRepositoryAdapter(JpaAIGenerationSpringRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(AIGeneration generation) {
        // AIGeneration is a record without ID or sessionId
        // We need to generate ID and get sessionId from context
        // This is a simplified implementation
        UUID id = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID(); // In real impl, extract from context

        JpaAIGeneration jpaEntity = toJpaEntity(id, sessionId, generation);
        repo.save(jpaEntity);
    }

    @Override
    public List<AIGeneration> findBySession(BriefingSessionId sessionId) {
        return repo.findBySessionOrderByCreatedAt(sessionId.value())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<AIGeneration> findBySessionAndType(BriefingSessionId sessionId, GenerationType type) {
        return repo.findBySessionAndType(sessionId.value(), type.name())
                .stream()
                .map(this::toDomain)
                .toList();
    }

    // ============ Domain → JPA Mapping ============

    private JpaAIGeneration toJpaEntity(UUID id, UUID sessionId, AIGeneration generation) {
        return new JpaAIGeneration(
                id,
                sessionId,
                generation.type().name(),
                generation.inputJson(),
                generation.outputJson(),
                generation.promptVersion(),
                generation.latencyMs(),
                generation.costUsd(),
                null, // modelUsed (not in domain record yet)
                java.time.Instant.now()
        );
    }

    // ============ JPA → Domain Mapping ============

    private AIGeneration toDomain(JpaAIGeneration jpa) {
        return new AIGeneration(
                GenerationType.valueOf(jpa.getGenerationType()),
                jpa.getInputJson(),
                jpa.getOutputJson(),
                jpa.getPromptVersion(),
                jpa.getLatencyMs() != null ? jpa.getLatencyMs() : 0L,
                jpa.getCostUsd() != null ? jpa.getCostUsd() : java.math.BigDecimal.ZERO
        );
    }
}
