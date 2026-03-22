package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * WorkspaceService: domain service for workspace lifecycle.
 *
 * Contains business logic (invariants, workflows).
 *
 * Invariants:
 * - Every workspace has exactly one OWNER
 * - Cannot remove last OWNER from workspace
 * - Workspace name must be unique
 */
public class WorkspaceService {
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository
    ) {
        this.workspaceRepository = Objects.requireNonNull(workspaceRepository);
        this.memberRepository = Objects.requireNonNull(memberRepository);
    }

    /**
     * Create a new workspace.
     *
     * Invariant: Name must be unique.
     * Creator becomes OWNER.
     *
     * @param ownerId user creating the workspace
     * @param name workspace name
     * @param niche business niche
     * @param toneSettings JSON tone configuration
     * @return newly created WorkspaceActive
     * @throws WorkspaceNameAlreadyExistsException if name exists
     */
    public WorkspaceActive createWorkspace(
            UserId ownerId,
            String name,
            String niche,
            String toneSettings
    ) {
        // Enforce invariant: name must be unique
        if (workspaceRepository.existsByName(name)) {
            throw new WorkspaceNameAlreadyExistsException("Workspace name already exists: " + name);
        }

        // Create workspace
        WorkspaceId workspaceId = WorkspaceId.generate();
        WorkspaceActive workspace = Workspace.create(workspaceId, ownerId, name, niche, toneSettings);

        // Persist
        workspaceRepository.save(workspace);

        // Add owner as member
        MemberActive ownerMember = WorkspaceMember.createActive(workspaceId, ownerId, Role.OWNER);
        memberRepository.save(ownerMember);

        return workspace;
    }

    /**
     * Invite a user to workspace.
     *
     * @param workspaceId target workspace
     * @param userId user to invite
     * @param role role to assign
     * @throws WorkspaceNotFoundException if workspace doesn't exist
     */
    public void inviteMember(WorkspaceId workspaceId, UserId userId, Role role) {
        // Verify workspace exists
        if (workspaceRepository.findById(workspaceId).isEmpty()) {
            throw new WorkspaceNotFoundException("Workspace not found: " + workspaceId);
        }

        // Check if already a member
        Optional<WorkspaceMember> existing = memberRepository.findByWorkspaceAndUser(workspaceId, userId);
        if (existing.isPresent()) {
            throw new MemberAlreadyExistsException("User already a member");
        }

        // Create invited member
        MemberInvited member = WorkspaceMember.createInvited(workspaceId, userId, role);
        memberRepository.save(member);

        // Note: In adapter layer, eventPublisher would publish WorkspaceMemberInvited event
    }

    /**
     * Change member role.
     *
     * Invariant: Cannot change last OWNER to another role.
     *
     * @throws CannotRemoveLastOwnerException if trying to demote only OWNER
     */
    public void updateMemberRole(WorkspaceId workspaceId, UserId userId, Role newRole) {
        Optional<WorkspaceMember> member = memberRepository.findByWorkspaceAndUser(workspaceId, userId);
        if (member.isEmpty()) {
            throw new MemberNotFoundException("Member not found");
        }

        // Invariant: check if removing last OWNER
        if (member.get().getRole() == Role.OWNER && newRole != Role.OWNER) {
            int ownerCount = memberRepository.countOwnersByWorkspace(workspaceId);
            if (ownerCount == 1) {
                throw new CannotRemoveLastOwnerException("Cannot demote the only OWNER");
            }
        }

        // Update role (in adapter layer, convert to new MemberActive with updated role)
        // For now, just log intent — actual update done via adapter repository
    }

    /**
     * Remove member from workspace.
     *
     * Invariant: Cannot remove last OWNER.
     *
     * @throws CannotRemoveLastOwnerException if removing only OWNER
     */
    public void removeMember(WorkspaceId workspaceId, UserId userId) {
        Optional<WorkspaceMember> member = memberRepository.findByWorkspaceAndUser(workspaceId, userId);
        if (member.isEmpty()) {
            throw new MemberNotFoundException("Member not found");
        }

        // Invariant: check if removing last OWNER
        if (member.get().getRole() == Role.OWNER) {
            int ownerCount = memberRepository.countOwnersByWorkspace(workspaceId);
            if (ownerCount == 1) {
                throw new CannotRemoveLastOwnerException("Cannot remove the only OWNER");
            }
        }

        // Delete member
        memberRepository.delete(workspaceId, userId);
    }

    /**
     * Get workspace by ID.
     */
    public Optional<Workspace> getWorkspaceById(WorkspaceId workspaceId) {
        return workspaceRepository.findById(workspaceId);
    }

    /**
     * List all members in workspace.
     */
    public List<WorkspaceMember> getWorkspaceMembers(WorkspaceId workspaceId) {
        return memberRepository.findAllByWorkspace(workspaceId);
    }
}
