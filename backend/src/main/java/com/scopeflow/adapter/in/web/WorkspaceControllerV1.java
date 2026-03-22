package com.scopeflow.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Workspace management endpoints for ScopeFlow API (v1).
 *
 * Handles:
 * - Create workspace (new tenant)
 * - List workspaces (for current user)
 * - Get workspace details
 * - Update workspace settings
 * - Member management (invite, list, update role, remove)
 *
 * All endpoints require JWT authentication (except registration).
 * All operations are workspace-scoped (multi-tenancy).
 */
@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces", description = "Workspace management and member RBAC")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceControllerV1 {

    /**
     * Create a new workspace.
     *
     * Only authenticated users can create.
     * Creator becomes OWNER (invariant).
     *
     * Request example:
     * ```json
     * {
     *   "name": "Acme Social Media Agency",
     *   "niche": "social-media",
     *   "tone_settings": {
     *     "tone": "professional",
     *     "industry": "marketing"
     *   }
     * }
     * ```
     *
     * Response: 201 CREATED with workspace details
     *
     * Invariant: Workspace name must be unique.
     * Error: 409 CONFLICT if name already exists
     */
    @PostMapping
    @Operation(
            summary = "Create a new workspace",
            description = "Create a new tenant workspace. Creator automatically becomes OWNER."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Workspace created",
                    content = @Content(schema = @Schema(implementation = WorkspaceResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Workspace name already exists (WORKSPACE-001)"
            ),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @RequestBody CreateWorkspaceRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * List all workspaces for current user.
     *
     * Filters by member status (current user must be active member).
     * Returns paginated list of workspaces with role info.
     *
     * Query params:
     * - page: 0-indexed page number (default: 0)
     * - size: results per page (default: 20, max: 100)
     * - sort: sort field (default: created_at DESC)
     *
     * Response: 200 OK with paginated list
     */
    @GetMapping
    @Operation(
            summary = "List user's workspaces",
            description = "Get all workspaces where current user is a member."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of workspaces",
            content = @Content(schema = @Schema(implementation = WorkspaceListResponse.class))
    )
    public ResponseEntity<WorkspaceListResponse> listWorkspaces(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Get workspace details.
     *
     * Requires: User must be member of workspace
     * Response: 200 OK with workspace details + member info
     */
    @GetMapping("/{workspaceId}")
    @Operation(
            summary = "Get workspace details",
            description = "Retrieve workspace info and current user's role in workspace."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Workspace details",
                    content = @Content(schema = @Schema(implementation = WorkspaceResponse.class))
            ),
            @ApiResponse(responseCode = "404", description = "Workspace not found"),
            @ApiResponse(responseCode = "403", description = "User not member of workspace")
    })
    public ResponseEntity<WorkspaceResponse> getWorkspace(
            @PathVariable UUID workspaceId
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Update workspace settings.
     *
     * Requires: User must have ADMIN or OWNER role
     * Response: 200 OK with updated workspace
     */
    @PutMapping("/{workspaceId}")
    @Operation(
            summary = "Update workspace settings",
            description = "Update workspace name, niche, tone settings. Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Workspace updated",
                    content = @Content(schema = @Schema(implementation = WorkspaceResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions (need ADMIN)")
    })
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable UUID workspaceId,
            @RequestBody UpdateWorkspaceRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    // ============ Member Management ============

    /**
     * Invite member to workspace.
     *
     * Requires: User must have ADMIN or OWNER role
     * Email must belong to existing user
     *
     * Request:
     * ```json
     * {
     *   "email": "team@example.com",
     *   "role": "ADMIN"
     * }
     * ```
     *
     * Response: 201 CREATED with membership details
     * Error: 409 CONFLICT if user already member
     */
    @PostMapping("/{workspaceId}/members")
    @Operation(
            summary = "Invite member to workspace",
            description = "Add user to workspace with specified role (ADMIN, MEMBER). Requires ADMIN role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Member invited",
                    content = @Content(schema = @Schema(implementation = WorkspaceMemberResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "User already member (WORKSPACE-004)"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<WorkspaceMemberResponse> inviteMember(
            @PathVariable UUID workspaceId,
            @RequestBody InviteMemberRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * List members in workspace.
     *
     * Requires: User must be member of workspace
     * Returns: All active members with their roles
     */
    @GetMapping("/{workspaceId}/members")
    @Operation(
            summary = "List workspace members",
            description = "Get all active members in workspace with their roles."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of members",
            content = @Content(schema = @Schema(implementation = WorkspaceMemberListResponse.class))
    )
    public ResponseEntity<WorkspaceMemberListResponse> listMembers(
            @PathVariable UUID workspaceId
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Update member role.
     *
     * Requires: User must have OWNER role
     * Invariant: Cannot change last OWNER's role
     *
     * Request: ```json { "role": "MEMBER" } ```
     * Response: 200 OK with updated membership
     */
    @PutMapping("/{workspaceId}/members/{memberId}/role")
    @Operation(
            summary = "Update member role",
            description = "Change member's role (ADMIN, MEMBER). Cannot demote last OWNER. Requires OWNER role."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Role updated",
                    content = @Content(schema = @Schema(implementation = WorkspaceMemberResponse.class))
            ),
            @ApiResponse(responseCode = "409", description = "Cannot remove last OWNER (WORKSPACE-003)"),
            @ApiResponse(responseCode = "403", description = "Requires OWNER role")
    })
    public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId,
            @RequestBody UpdateMemberRoleRequest request
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    /**
     * Remove member from workspace.
     *
     * Requires: User must have OWNER role
     * Invariant: Cannot remove last OWNER
     *
     * Response: 204 NO_CONTENT
     */
    @DeleteMapping("/{workspaceId}/members/{memberId}")
    @Operation(
            summary = "Remove member from workspace",
            description = "Remove user from workspace. Cannot remove last OWNER. Requires OWNER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Member removed"),
            @ApiResponse(responseCode = "409", description = "Cannot remove last OWNER (WORKSPACE-003)"),
            @ApiResponse(responseCode = "403", description = "Requires OWNER role")
    })
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID workspaceId,
            @PathVariable UUID memberId
    ) {
        throw new UnsupportedOperationException("Implement in adapter layer");
    }

    // ============ DTOs ============

    public record CreateWorkspaceRequest(
            String name,
            String niche,
            Object tone_settings
    ) {
    }

    public record UpdateWorkspaceRequest(
            String name,
            String niche,
            Object tone_settings
    ) {
    }

    public record InviteMemberRequest(
            String email,
            String role
    ) {
    }

    public record UpdateMemberRoleRequest(
            String role
    ) {
    }

    public record WorkspaceResponse(
            UUID id,
            String name,
            String niche,
            String status,
            Object tone_settings,
            String owner_id,
            String user_role,
            String created_at,
            String updated_at
    ) {
    }

    public record WorkspaceMemberResponse(
            UUID id,
            String user_id,
            String email,
            String full_name,
            String role,
            String status,
            String joined_at
    ) {
    }

    public record WorkspaceListResponse(
            List<WorkspaceResponse> content,
            int page,
            int size,
            int total_elements,
            int total_pages
    ) {
    }

    public record WorkspaceMemberListResponse(
            List<WorkspaceMemberResponse> members,
            int total
    ) {
    }
}
