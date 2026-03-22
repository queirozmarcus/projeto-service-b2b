package com.scopeflow.adapter.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WorkspaceController — Workspace Management.
 *
 * Endpoints: create workspace, list, update, invite members, manage roles.
 */
@RestController
@RequestMapping("/workspaces")
public class WorkspaceController {

  /**
   * POST /workspaces — Create new workspace.
   *
   * @param request CreateWorkspaceRequest
   * @return WorkspaceResponse with workspace details
   */
  @PostMapping
  public ResponseEntity<WorkspaceResponse> createWorkspace(
      @RequestBody CreateWorkspaceRequest request) {
    // TODO: Implement workspace creation logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /workspaces — List workspaces for current user.
   *
   * @return List of WorkspaceResponse
   */
  @GetMapping
  public ResponseEntity<List<WorkspaceResponse>> listWorkspaces() {
    // TODO: Implement list logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * GET /workspaces/{workspaceId} — Get workspace details.
   *
   * @param workspaceId UUID of workspace
   * @return WorkspaceResponse
   */
  @GetMapping("/{workspaceId}")
  public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable UUID workspaceId) {
    // TODO: Implement fetch logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * PUT /workspaces/{workspaceId} — Update workspace.
   *
   * @param workspaceId UUID of workspace
   * @param request UpdateWorkspaceRequest
   * @return WorkspaceResponse
   */
  @PutMapping("/{workspaceId}")
  public ResponseEntity<WorkspaceResponse> updateWorkspace(
      @PathVariable UUID workspaceId, @RequestBody UpdateWorkspaceRequest request) {
    // TODO: Implement update logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * POST /workspaces/{workspaceId}/members — Invite user to workspace.
   *
   * @param workspaceId UUID of workspace
   * @param request InviteRequest
   * @return WorkspaceMemberResponse
   */
  @PostMapping("/{workspaceId}/members")
  public ResponseEntity<WorkspaceMemberResponse> inviteMember(
      @PathVariable UUID workspaceId, @RequestBody InviteRequest request) {
    // TODO: Implement invite logic
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  /**
   * GET /workspaces/{workspaceId}/members — List workspace members.
   *
   * @param workspaceId UUID of workspace
   * @return List of WorkspaceMemberResponse
   */
  @GetMapping("/{workspaceId}/members")
  public ResponseEntity<List<WorkspaceMemberResponse>> listMembers(@PathVariable UUID workspaceId) {
    // TODO: Implement list members logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * PUT /workspaces/{workspaceId}/members/{memberId}/role — Update member role.
   *
   * @param workspaceId UUID of workspace
   * @param memberId UUID of member
   * @param request UpdateMemberRoleRequest
   * @return WorkspaceMemberResponse
   */
  @PutMapping("/{workspaceId}/members/{memberId}/role")
  public ResponseEntity<WorkspaceMemberResponse> updateMemberRole(
      @PathVariable UUID workspaceId,
      @PathVariable UUID memberId,
      @RequestBody UpdateMemberRoleRequest request) {
    // TODO: Implement role update logic
    throw new UnsupportedOperationException("Not yet implemented");
  }

  /**
   * DELETE /workspaces/{workspaceId}/members/{memberId} — Remove member from workspace.
   *
   * @param workspaceId UUID of workspace
   * @param memberId UUID of member
   * @return ResponseEntity with no content
   */
  @DeleteMapping("/{workspaceId}/members/{memberId}")
  public ResponseEntity<Void> removeMember(
      @PathVariable UUID workspaceId, @PathVariable UUID memberId) {
    // TODO: Implement member removal logic
    return ResponseEntity.noContent().build();
  }

  // ============================================================================
  // DTOs
  // ============================================================================

  public record CreateWorkspaceRequest(String name, String description, String logoUrl) {}

  public record UpdateWorkspaceRequest(String name, String description, String logoUrl) {}

  public record WorkspaceResponse(
      UUID id,
      String name,
      String slug,
      String description,
      String logoUrl,
      String status,
      long memberCount) {}

  public record InviteRequest(String email, String role) {}

  public record UpdateMemberRoleRequest(String role) {}

  public record WorkspaceMemberResponse(
      UUID id, UUID userId, String email, String fullName, String role, String status) {}
}
