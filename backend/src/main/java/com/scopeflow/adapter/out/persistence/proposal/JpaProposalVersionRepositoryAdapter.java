package com.scopeflow.adapter.out.persistence.proposal;

import com.scopeflow.core.domain.proposal.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing ProposalVersionRepository domain port.
 */
@Component
@Transactional(readOnly = true)
public class JpaProposalVersionRepositoryAdapter implements ProposalVersionRepository {

    private final JpaProposalVersionSpringRepository springRepo;
    private final ProposalScopeJsonMapper scopeMapper;

    public JpaProposalVersionRepositoryAdapter(
            JpaProposalVersionSpringRepository springRepo,
            ProposalScopeJsonMapper scopeMapper
    ) {
        this.springRepo = springRepo;
        this.scopeMapper = scopeMapper;
    }

    @Override
    @Transactional
    public void save(ProposalVersion version) {
        // Version is immutable: always insert
        springRepo.save(fromDomain(version));
    }

    @Override
    public Optional<ProposalVersion> findById(ProposalVersionId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<ProposalVersion> findByProposalId(ProposalId proposalId) {
        return springRepo.findByProposalIdOrderByCreatedAtDesc(proposalId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    private ProposalVersion toDomain(JpaProposalVersion entity) {
        return new ProposalVersion(
                new ProposalVersionId(entity.getId()),
                ProposalId.of(entity.getProposalId()),
                scopeMapper.fromJson(entity.getScopeJson()),
                entity.getCreatedAt(),
                entity.getCreatedBy()
        );
    }

    private JpaProposalVersion fromDomain(ProposalVersion version) {
        return new JpaProposalVersion(
                version.id().value(),
                version.proposalId().value(),
                scopeMapper.toJson(version.scope()),
                version.createdAt(),
                version.createdBy()
        );
    }
}
