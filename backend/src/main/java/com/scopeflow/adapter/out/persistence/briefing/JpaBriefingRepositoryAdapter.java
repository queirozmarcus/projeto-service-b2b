package com.scopeflow.adapter.out.persistence.briefing;

import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA implementation of BriefingSessionRepository domain port.
 * Converts between domain objects and JPA entities.
 */
@Component
public class JpaBriefingRepositoryAdapter implements BriefingSessionRepository {

    private final JpaBriefingSessionSpringRepository sessionRepo;
    private final JpaBriefingAnswerSpringRepository answerRepo;

    public JpaBriefingRepositoryAdapter(
            JpaBriefingSessionSpringRepository sessionRepo,
            JpaBriefingAnswerSpringRepository answerRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.answerRepo = answerRepo;
    }

    @Override
    public Optional<BriefingSession> findById(BriefingSessionId id) {
        return sessionRepo.findById(id.value())
                .map(this::toDomain);
    }

    @Override
    public void save(BriefingSession session) {
        JpaBriefingSession jpaEntity = toJpaEntity(session);
        sessionRepo.save(jpaEntity);
    }

    @Override
    public Optional<BriefingSession> findActiveByClientAndService(ClientId clientId, ServiceType serviceType) {
        return sessionRepo.findActiveByClientAndService(clientId.value(), serviceType.name())
                .map(this::toDomain);
    }

    @Override
    public List<BriefingSession> findByWorkspaceAndStatus(WorkspaceId workspaceId, String status) {
        return sessionRepo.findByWorkspaceAndStatus(workspaceId.value(), status)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countAnswers(BriefingSessionId sessionId) {
        return sessionRepo.countAnswers(sessionId.value());
    }

    /**
     * Find by public token (for public client access).
     */
    public Optional<BriefingSession> findByPublicToken(PublicToken token) {
        return sessionRepo.findByPublicToken(token.value())
                .map(this::toDomain);
    }

    /**
     * Find with pagination and filters.
     */
    public Page<BriefingSession> findByWorkspaceWithFilters(
            WorkspaceId workspaceId,
            String status,
            String serviceType,
            Instant createdAfter,
            Pageable pageable
    ) {
        return sessionRepo.findByWorkspaceWithFilters(
                workspaceId.value(),
                status,
                serviceType,
                createdAfter,
                pageable
        ).map(this::toDomain);
    }

    // ============ Domain → JPA Mapping ============

    private JpaBriefingSession toJpaEntity(BriefingSession session) {
        return new JpaBriefingSession(
                session.getId().value(),
                session.getWorkspaceId().value(),
                session.getClientId().value(),
                session.getServiceType().name(),
                session.status(),
                session.getPublicToken().value(),
                extractCompletionScore(session),
                extractAiAnalysis(session),
                extractAbandonReason(session),
                session.getCreatedAt(),
                session.getUpdatedAt()
        );
    }

    private Integer extractCompletionScore(BriefingSession session) {
        if (session instanceof BriefingCompleted completed) {
            return completed.getCompletionScore().score();
        }
        return null;
    }

    private String extractAiAnalysis(BriefingSession session) {
        if (session instanceof BriefingCompleted completed) {
            // Serialize CompletionScore gaps to JSON
            var gaps = completed.getCompletionScore().gapsIdentified();
            if (gaps.isEmpty()) {
                return null;
            }
            // Simple JSON array (in real impl, use Jackson ObjectMapper)
            return "{\"gaps\":[" + String.join(",", gaps.stream().map(g -> "\"" + g + "\"").toList()) + "]}";
        }
        return null;
    }

    private String extractAbandonReason(BriefingSession session) {
        if (session instanceof BriefingAbandoned abandoned) {
            return abandoned.getAbandonReason();
        }
        return null;
    }

    // ============ JPA → Domain Mapping ============

    private BriefingSession toDomain(JpaBriefingSession jpa) {
        BriefingSessionId id = new BriefingSessionId(jpa.getId());
        WorkspaceId workspaceId = new WorkspaceId(jpa.getWorkspaceId());
        ClientId clientId = new ClientId(jpa.getClientId());
        ServiceType serviceType = ServiceType.valueOf(jpa.getServiceType());
        PublicToken publicToken = new PublicToken(jpa.getPublicToken());
        Instant createdAt = jpa.getCreatedAt();
        Instant updatedAt = jpa.getUpdatedAt();

        return switch (jpa.getStatus()) {
            case "IN_PROGRESS" -> new BriefingInProgress(
                    id,
                    workspaceId,
                    clientId,
                    serviceType,
                    publicToken,
                    createdAt,
                    updatedAt,
                    new BriefingProgress(0, 0, 0) // Calculate progress dynamically via countAnswers
            );
            case "COMPLETED" -> {
                Integer score = jpa.getCompletionScore();
                if (score == null) {
                    throw new IllegalStateException("COMPLETED session must have completion score");
                }
                yield new BriefingCompleted(
                        id,
                        workspaceId,
                        clientId,
                        serviceType,
                        publicToken,
                        createdAt,
                        updatedAt,
                        new CompletionScore(score, List.of()) // Parse gaps from aiAnalysis if needed
                );
            }
            case "ABANDONED" -> {
                String reason = jpa.getAbandonedReason() != null ? jpa.getAbandonedReason() : "No reason provided";
                yield new BriefingAbandoned(
                        id,
                        workspaceId,
                        clientId,
                        serviceType,
                        publicToken,
                        createdAt,
                        updatedAt,
                        reason
                );
            }
            default -> throw new IllegalStateException("Unknown status: " + jpa.getStatus());
        };
    }
}
