package com.scopeflow.adapter.out.persistence.workspace;

import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.workspace.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA adapter implementing WorkspaceMemberRepository domain port.
 */
@Component
public class JpaWorkspaceMemberRepositoryAdapter implements WorkspaceMemberRepository {

    private final JpaWorkspaceMemberSpringRepository springRepo;

    public JpaWorkspaceMemberRepositoryAdapter(JpaWorkspaceMemberSpringRepository springRepo) {
        this.springRepo = springRepo;
    }

    @Override
    public void save(WorkspaceMember member) {
        springRepo.findByWorkspaceIdAndUserId(
                member.getWorkspaceId().value(), member.getUserId().value()
        ).ifPresentOrElse(
                existing -> {
                    existing.setRole(member.getRole().name());
                    existing.setStatus(member.status());
                    existing.setUpdatedAt(Instant.now());
                    springRepo.save(existing);
                },
                () -> springRepo.save(fromDomain(member))
        );
    }

    @Override
    public Optional<WorkspaceMember> findByWorkspaceAndUser(WorkspaceId workspaceId, UserId userId) {
        return springRepo.findByWorkspaceIdAndUserId(workspaceId.value(), userId.value())
                .map(this::toDomain);
    }

    @Override
    public List<WorkspaceMember> findAllByWorkspace(WorkspaceId workspaceId) {
        return springRepo.findByWorkspaceId(workspaceId.value()).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WorkspaceMember> findActiveMembers(WorkspaceId workspaceId) {
        return springRepo.findByWorkspaceIdAndStatus(workspaceId.value(), "ACTIVE").stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int countOwnersByWorkspace(WorkspaceId workspaceId) {
        return springRepo.countActiveOwners(workspaceId.value());
    }

    @Override
    @Transactional
    public void delete(WorkspaceId workspaceId, UserId userId) {
        springRepo.findByWorkspaceIdAndUserId(workspaceId.value(), userId.value())
                .ifPresent(entity -> {
                    entity.setStatus("LEFT");
                    entity.setUpdatedAt(Instant.now());
                    springRepo.save(entity);
                });
    }

    // ============ JPA → Domain ============

    private WorkspaceMember toDomain(JpaWorkspaceMember entity) {
        WorkspaceId workspaceId = new WorkspaceId(entity.getWorkspaceId());
        UserId userId = new UserId(entity.getUserId());
        Role role = Role.valueOf(entity.getRole());
        return switch (entity.getStatus()) {
            case "ACTIVE" -> new MemberActive(workspaceId, userId, role,
                    entity.getJoinedAt(), entity.getUpdatedAt());
            case "INVITED" -> new MemberInvited(workspaceId, userId, role,
                    entity.getJoinedAt(), entity.getUpdatedAt());
            case "LEFT" -> new MemberLeft(workspaceId, userId, role,
                    entity.getJoinedAt(), entity.getUpdatedAt());
            default -> throw new IllegalStateException("Unknown member status: " + entity.getStatus());
        };
    }

    // ============ Domain → JPA ============

    private JpaWorkspaceMember fromDomain(WorkspaceMember member) {
        return new JpaWorkspaceMember(
                UUID.randomUUID(), // composite key — generate id here
                member.getWorkspaceId().value(),
                member.getUserId().value(),
                member.getRole().name(),
                member.status(),
                member.getJoinedAt(),
                member.getUpdatedAt()
        );
    }
}
