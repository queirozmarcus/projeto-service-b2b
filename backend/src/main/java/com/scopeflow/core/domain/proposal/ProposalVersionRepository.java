package com.scopeflow.core.domain.proposal;

import java.util.List;
import java.util.Optional;

/**
 * ProposalVersionRepository port (domain layer).
 */
public interface ProposalVersionRepository {

    void save(ProposalVersion version);

    Optional<ProposalVersion> findById(ProposalVersionId id);

    List<ProposalVersion> findByProposalId(ProposalId proposalId);
}
