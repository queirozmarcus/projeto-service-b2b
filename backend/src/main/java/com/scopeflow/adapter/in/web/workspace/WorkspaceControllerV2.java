package com.scopeflow.adapter.in.web.workspace;

import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.adapter.in.web.workspace.dto.*;
import com.scopeflow.core.domain.user.*;
import com.scopeflow.core.domain.workspace.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public WorkspaceControllerV2(
            WorkspaceService workspaceService,
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        this.workspaceService = workspaceService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
     * Note: Returns current workspace from JWT context as a simplified approach.
     */
    @GetMapping
    @Operation(summary = "List workspaces for current user")
    public List<WorkspaceResponse> list() {
        UUID workspaceId = SecurityUtil.getWorkspaceId();
        Workspace workspace = workspaceService.getWorkspaceById(new WorkspaceId(workspaceId))
                .orElseThrow(() -> new WorkspaceNotFoundException("Workspace not found: " + workspaceId));

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspace.getId());
        return List.of(WorkspaceResponse.from(workspace, members.stream().map(MemberResponse::from).toList()));
    }

    /**
     * POST /workspaces/{id}/members/invite
     * Invite a user by email with a role.
     *
     * If the user already exists (by email), they are added directly.
     * If the user does not exist, a new INACTIVE user is created and
     * an invite link would be sent async (not yet implemented — logged for now).
     */
    @PostMapping("/{workspaceId}/members/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite member to workspace")
    public MemberResponse inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request
    ) {
        requireOwnerOrAdmin(workspaceId);

        WorkspaceId wsId = new WorkspaceId(workspaceId);
        Email email = new Email(request.email());

        // Lookup existing user by email, or create a new INACTIVE user
        User invitedUser = userService.getUserByEmail(email)
                .orElseGet(() -> {
                    // User does not exist — create as INACTIVE pending invite acceptance
                    UserId newUserId = UserId.generate();
                    // Temporary password hash (INACTIVE user cannot login until they set a real one)
                    String tempHashValue = passwordEncoder.encode(UUID.randomUUID().toString());
                    PasswordHash tempHash = new PasswordHash(tempHashValue);
                    String displayName = extractDisplayNameFromEmail(email.normalized());
                    UserInactive newUser = User.createInvited(newUserId, email, tempHash, displayName);
                    userService.saveInvitedUser(newUser);
                    log.info("New invited user created: userId={}, email={}", newUserId.value(), email.normalized());
                    return newUser;
                });

        workspaceService.inviteMember(wsId, invitedUser.getId(), request.role());

        // TODO: publish WorkspaceMemberInvited event → async email with accept link
        log.info("Member invited: workspaceId={}, email={}, role={}, userId={}",
                workspaceId, request.email(), request.role(), invitedUser.getId().value());

        return new MemberResponse(invitedUser.getId().value(), request.role().name(), "INVITED", null);
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

    // ============ Private helpers ============

    private void requireOwnerOrAdmin(UUID workspaceId) {
        if (!SecurityUtil.hasRole("OWNER") && !SecurityUtil.hasRole("ADMIN")) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only OWNER or ADMIN can perform this action"
            );
        }
    }

    /**
     * Extract a display name from email address (e.g., "john.doe@example.com" → "John Doe").
     * Used as default name for newly invited users.
     */
    private String extractDisplayNameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return localPart.replace(".", " ").replace("_", " ")
                .chars()
                .collect(StringBuilder::new,
                        (sb, c) -> {
                            if (sb.isEmpty() || sb.charAt(sb.length() - 1) == ' ') {
                                sb.append(Character.toUpperCase(c));
                            } else {
                                sb.append((char) c);
                            }
                        },
                        StringBuilder::append)
                .toString();
    }
}
