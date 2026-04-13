package com.scopeflow.adapter.out.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for JpaWorkspaceMember.
 */
public interface JpaWorkspaceMemberSpringRepository extends JpaRepository<JpaWorkspaceMember, UUID> {

    Optional<JpaWorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    List<JpaWorkspaceMember> findByWorkspaceId(UUID workspaceId);

    List<JpaWorkspaceMember> findByWorkspaceIdAndStatus(UUID workspaceId, String status);

    @Query("SELECT COUNT(m) FROM JpaWorkspaceMember m WHERE m.workspaceId = :workspaceId AND m.role = 'OWNER' AND m.status = 'ACTIVE'")
    int countActiveOwners(@Param("workspaceId") UUID workspaceId);

    void deleteByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
