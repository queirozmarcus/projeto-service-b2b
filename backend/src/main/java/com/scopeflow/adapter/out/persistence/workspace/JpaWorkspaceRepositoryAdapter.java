package com.scopeflow.adapter.out.persistence.workspace;

import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.workspace.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * JPA adapter implementing WorkspaceRepository domain port.
 */
@Component
@Transactional(readOnly = true)
public class JpaWorkspaceRepositoryAdapter implements WorkspaceRepository {

    private final JpaWorkspaceSpringRepository springRepo;

    public JpaWorkspaceRepositoryAdapter(JpaWorkspaceSpringRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    @Transactional
    public void save(Workspace workspace) {
        springRepo.findById(workspace.getId().value()).ifPresentOrElse(
                existing -> {
                    existing.setName(workspace.getName());
                    existing.setNiche(workspace.getNiche());
                    existing.setToneSettings(workspace.getToneSettings());
                    existing.setStatus(workspace.status());
                    existing.setUpdatedAt(Instant.now());
                    springRepo.save(existing);
                },
                () -> springRepo.save(fromDomain(workspace))
        );
    }

    @Override
    public Optional<Workspace> findById(WorkspaceId id) {
        return springRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return springRepo.existsByName(name);
    }

    @Override
    @Transactional
    public void delete(WorkspaceId id) {
        springRepo.findById(id.value()).ifPresent(entity -> {
            entity.setStatus("SUSPENDED");
            entity.setUpdatedAt(Instant.now());
            springRepo.save(entity);
        });
    }

    // ============ JPA → Domain ============

    private Workspace toDomain(JpaWorkspace entity) {
        WorkspaceId id = new WorkspaceId(entity.getId());
        UserId ownerId = new UserId(entity.getOwnerId());
        return switch (entity.getStatus()) {
            case "ACTIVE" -> new WorkspaceActive(
                    id, ownerId, entity.getName(), entity.getNiche(),
                    entity.getToneSettings(), entity.getCreatedAt(), entity.getUpdatedAt()
            );
            case "SUSPENDED" -> new WorkspaceSuspended(
                    id, ownerId, entity.getName(), entity.getNiche(),
                    entity.getToneSettings(), entity.getCreatedAt(), entity.getUpdatedAt()
            );
            default -> throw new IllegalStateException("Unknown workspace status: " + entity.getStatus());
        };
    }

    // ============ Domain → JPA ============

    private JpaWorkspace fromDomain(Workspace workspace) {
        return new JpaWorkspace(
                workspace.getId().value(),
                workspace.getOwnerId().value(),
                workspace.getName(),
                workspace.getNiche(),
                workspace.getToneSettings(),
                workspace.status(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }
}
