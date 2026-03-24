package com.scopeflow.adapter.out.persistence.proposal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaProposalVersion.
 */
public interface JpaProposalVersionSpringRepository extends JpaRepository<JpaProposalVersion, UUID> {

    List<JpaProposalVersion> findByProposalIdOrderByCreatedAtDesc(UUID proposalId);
}
