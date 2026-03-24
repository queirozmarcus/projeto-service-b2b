package com.scopeflow.adapter.in.web.workspace;

import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.adapter.in.web.workspace.dto.*;
import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.workspace.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workspace management: create, list, update, invite members, manage roles.
 *
 * Path: /api/v1/workspaces
 * All endpoints require authentication (JWT).
 * RBAC enforced at use case level (WorkspaceService).
 */
@RestController
@RequestMapping("/workspaces")
@Tag(name = "Workspaces", description = "Workspace and member management")
public class WorkspaceControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceControllerV2.class);

    private final WorkspaceService workspaceService;

    public WorkspaceControllerV2(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    /**
     * POST /workspaces
     * Create a new workspace. Authenticated user becomes OWNER.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new workspace")
    public WorkspaceResponse create(@Valid @RequestBody CreateWorkspaceRequest request) {
        UserId ownerId = new UserId(SecurityUtil.getUserId());

        WorkspaceActive workspace = workspaceService.createWorkspace(
                ownerId,
                request.name(),
                request.niche(),
                request.toneSettings()
        );

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspace.getId());
        log.info("Workspace created: workspaceId={}, ownerId={}", workspace.getId().value(), ownerId.value());

        return WorkspaceResponse.from(workspace, members.stream().map(MemberResponse::from).toList());
    }

    /**
     * GET /workspaces/{id}
     * Get workspace details.
     */
    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get workspace by ID")
    public WorkspaceResponse getById(@PathVariable UUID workspaceId) {
        Workspace workspace = workspaceService.getWorkspaceById(new WorkspaceId(workspaceId))
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found: " + workspaceId));

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspace.getId());
        return WorkspaceResponse.from(workspace, members.stream().map(MemberResponse::from).toList());
    }

    /**
     * GET /workspaces
     * List all workspaces where current user is a member.
     *
     * Note: This requires a user-scoped query (not in current WorkspaceService).
     * Returns current workspace from JWT context as a simplified approach.
     */
    @GetMapping
    @Operation(summary = "List workspaces for current user")
    public List<WorkspaceResponse> list() {
        // Simplified: return workspace from JWT context
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        Workspace workspace = workspaceService.getWorkspaceById(new WorkspaceId(workspaceId))
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found: " + workspaceId));

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspace.getId());
        return List.of(WorkspaceResponse.from(workspace, members.stream().map(MemberResponse::from).toList()));
    }

    /**
     * POST /workspaces/{id}/members/invite
     * Invite a user by email with a role.
     */
    @PostMapping("/{workspaceId}/members/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite member to workspace")
    public MemberResponse inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        // Enforce RBAC: only OWNER or ADMIN can invite
        requireOwnerOrAdmin(workspaceId);

        // Invite by finding user by email (simplified — real impl would send email invite)
        // For now: create a placeholder invited member record
        // In production, would look up user by email or send invite link
        WorkspaceId wsId = new WorkspaceId(workspaceId);

        // Placeholder userId — in production, look up or create user by email
        UserId invitedUserId = UserId.generate();
        workspaceService.inviteMember(wsId, invitedUserId, request.role());

        log.info("Member invited: workspaceId={}, email={}, role={}",
                workspaceId, request.email(), request.role());

        return new MemberResponse(invitedUserId.value(), request.role().name(), "INVITED", null);
    }

    /**
     * PUT /workspaces/{id}/members/{memberId}/role
     * Update a member's role.
     */
    @PutMapping("/{workspaceId}/members/{memberId}/role")
    @Operation(summary = "Update member role")
    public MemberResponse updateRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        requireOwnerOrAdmin(workspaceId);

        workspaceService.updateMemberRole(
                new WorkspaceId(workspaceId),
                new UserId(memberId),
                request.role()
        );

        return new MemberResponse(memberId, request.role().name(), "ACTIVE", null);
    }

    /**
     * DELETE /workspaces/{id}/members/{memberId}
     * Remove a member from workspace.
     */
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove member from workspace")
    public void removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId
    ) {
        requireOwnerOrAdmin(workspaceId);

        workspaceService.removeMember(
                new WorkspaceId(workspaceId),
                new UserId(memberId)
        );

        log.info("Member removed: workspaceId={}, memberId={}", workspaceId, memberId);
    }

    /**
     * GET /workspaces/{id}/members
     * List all members of a workspace.
     */
    @GetMapping("/{workspaceId}/members")
    @Operation(summary = "List workspace members")
    public List<MemberResponse> listMembers(@PathVariable UUID workspaceId) {
        return workspaceService.getWorkspaceMembers(new WorkspaceId(workspaceId))
                .stream()
                .map(MemberResponse::from)
                .toList();
    }

    // ============ RBAC helpers ============

    private void requireOwnerOrAdmin(UUID workspaceId) {
        if (!SecurityUtil.hasRole("OWNER") && !SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only OWNER or ADMIN can perform this action"
            );
        }
    }
}
