package com.scopeflow.adapter.out.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaWorkspace.
 */
public interface JpaWorkspaceSpringRepository extends JpaRepository<JpaWorkspace, UUID> {

    boolean existsByName(String name);

    List<JpaWorkspace> findByOwnerId(UUID ownerId);

    List<JpaWorkspace> findByOwnerIdAndStatus(UUID ownerId, String status);
}
