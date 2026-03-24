package com.scopeflow.adapter.out.persistence.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing ProposalRepository domain port.
 */
@Component
@Transactional(readOnly = true)
public class JpaProposalRepositoryAdapter implements ProposalRepository {

    private final JpaProposalSpringRepository springRepo;
    private final ProposalScopeJsonMapper scopeMapper;

    public JpaProposalRepositoryAdapter(
            JpaProposalSpringRepository springRepo,
            ProposalScopeJsonMapper scopeMapper
    ) {
        this.springRepo = springRepo;
        this.scopeMapper = scopeMapper;
    }

    @Override
    @Transactional
    public void save(Proposal proposal) {
        springRepo.findById(proposal.getId().value()).ifPresentOrElse(
                existing -> {
                    existing.setStatus(proposal.status().name());
                    existing.setScopeJson(scopeMapper.toJson(proposal.getScope()));
                    existing.setUpdatedAt(Instant.now());
                    springRepo.save(existing);
                },
                () -> springRepo.save(fromDomain(proposal))
        );
    }

    @Override
    public Optional<Proposal> findById(ProposalId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Proposal> findByWorkspaceId(WorkspaceId workspaceId) {
        return springRepo.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Proposal> findByWorkspaceIdAndStatus(WorkspaceId workspaceId, ProposalStatus status) {
        return springRepo.findByWorkspaceIdAndStatus(workspaceId.value(), status.name()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Proposal> findByClientIdAndWorkspaceId(UUID clientId, WorkspaceId workspaceId) {
        return springRepo.findByClientIdAndWorkspaceId(clientId, workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(ProposalId id) {
        springRepo.deleteById(id.value());
    }

    // ============ JPA → Domain ============

    private Proposal toDomain(JpaProposal entity) {
        ProposalId id = ProposalId.of(entity.getId());
        WorkspaceId workspaceId = new WorkspaceId(entity.getWorkspaceId());
        BriefingSessionId briefingId = new BriefingSessionId(entity.getBriefingId());
        ProposalScope scope = scopeMapper.fromJson(entity.getScopeJson());

        return switch (entity.getStatus()) {
            case "DRAFT" -> new ProposalDraft(
                    id, workspaceId, entity.getClientId(), briefingId,
                    entity.getProposalName(), scope, entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "PUBLISHED" -> new ProposalPublished(
                    id, workspaceId, entity.getClientId(), briefingId,
                    entity.getProposalName(), scope, entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "APPROVED" -> new ProposalApproved(
                    id, workspaceId, entity.getClientId(), briefingId,
                    entity.getProposalName(), scope, entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "REJECTED" -> new ProposalRejected(
                    id, workspaceId, entity.getClientId(), briefingId,
                    entity.getProposalName(), scope, entity.getCreatedAt(), entity.getUpdatedAt()
            );
            default -> throw new IllegalStateException("Unknown proposal status: " + entity.getStatus());
        };
    }

    // ============ Domain → JPA ============

    private JpaProposal fromDomain(Proposal proposal) {
        return new JpaProposal(
                proposal.getId().value(),
                proposal.getWorkspaceId().value(),
                proposal.getClientId(),
                proposal.getBriefingId().value(),
                proposal.getProposalName(),
                proposal.status().name(),
                scopeMapper.toJson(proposal.getScope()),
                proposal.getCreatedAt(),
                proposal.getUpdatedAt()
        );
    }
}
