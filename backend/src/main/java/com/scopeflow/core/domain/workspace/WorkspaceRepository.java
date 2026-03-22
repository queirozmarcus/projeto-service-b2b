package com.scopeflow.core.domain.workspace;

import java.util.Optional;

/**
 * WorkspaceRepository interface (domain layer, port).
 */
public interface WorkspaceRepository {

    /**
     * Save a workspace (create or update).
     */
    void save(Workspace workspace);

    /**
     * Find workspace by ID.
     */
    Optional<Workspace> findById(WorkspaceId id);

    /**
     * Check if workspace name is unique (within user account).
     */
    boolean existsByName(String name);

    /**
     * Delete a workspace (soft or hard — implement later).
     */
    void delete(WorkspaceId id);
}
