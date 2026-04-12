package com.scopeflow.adapter.in.web.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.adapter.in.web.workspace.dto.*;
import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.workspace.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
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

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private final WorkspaceService workspaceService;
    private final RestTemplate authServiceRestTemplate;
    private final ObjectMapper objectMapper;

    public WorkspaceControllerV2(
            WorkspaceService workspaceService,
            RestTemplate authServiceRestTemplate,
            ObjectMapper objectMapper
    ) {
        this.workspaceService = workspaceService;
        this.authServiceRestTemplate = authServiceRestTemplate;
        this.objectMapper = objectMapper;
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
     * If the user already exists in user-service (by email), they are added directly.
     * If the user does not exist, a new INACTIVE user is created in user-service and
     * an invite link would be sent async (not yet implemented — logged for now).
     */
    @PostMapping("/{workspaceId}/members/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite member to workspace")
    public MemberResponse inviteMember(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody InviteMemberRequest request,
            HttpServletRequest httpRequest
    ) {
        requireOwnerOrAdmin(workspaceId);

        WorkspaceId wsId = new WorkspaceId(workspaceId);

        // Step 1: lookup existing user by email in user-service
        UUID invitedUserId = lookupOrCreateUser(request.email(), request.role(), httpRequest);

        // Step 2: add user to workspace
        workspaceService.inviteMember(wsId, new UserId(invitedUserId), request.role());

        // TODO: publish WorkspaceMemberInvited event → async email with accept link
        log.info("Member invited: workspaceId={}, email={}, role={}, userId={}",
                workspaceId, request.email(), request.role(), invitedUserId);

        return new MemberResponse(invitedUserId, request.role().name(), "INVITED", null);
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
     * Lookup user by email in user-service. If not found (404), create as INACTIVE invited user.
     *
     * @return UUID of the user (existing or newly created)
     */
    private UUID lookupOrCreateUser(String email, com.scopeflow.core.domain.workspace.Role role, HttpServletRequest request) {
        // Try to find existing user by email
        ResponseEntity<String> lookupResponse = proxyGetWithAuth("/users/by-email/" + email, request);

        if (lookupResponse.getStatusCode() == HttpStatus.OK) {
            return extractIdFromResponse(lookupResponse.getBody());
        }

        if (lookupResponse.getStatusCode() == HttpStatus.NOT_FOUND) {
            // User does not exist — create as INACTIVE pending invite acceptance
            UUID invitedByUserId = SecurityUtil.getUserId();
            Map<String, Object> body = Map.of(
                    "email", email,
                    "invitedByUserId", invitedByUserId.toString(),
                    "role", role.name()
            );
            ResponseEntity<String> createResponse = proxyPostWithAuth("/users/invited", body, request);

            if (createResponse.getStatusCode() == HttpStatus.CREATED) {
                log.info("New invited user created in user-service: email={}", email);
                return extractIdFromResponse(createResponse.getBody());
            }

            if (createResponse.getStatusCode() == HttpStatus.CONFLICT) {
                throw new com.scopeflow.core.domain.user.DuplicateEmailException(
                        new com.scopeflow.core.domain.user.Email(email));
            }

            throw new RuntimeException("Failed to create invited user in user-service: status=" + createResponse.getStatusCode());
        }

        throw new RuntimeException("Unexpected response from user-service lookup: status=" + lookupResponse.getStatusCode());
    }

    /**
     * Extract the "id" field (UUID) from a user-service JSON response body.
     */
    private UUID extractIdFromResponse(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return UUID.fromString(node.get("id").asText());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse user id from user-service response", e);
        }
    }

    // ============ Proxy helpers ============

    private ResponseEntity<String> proxyGetWithAuth(String path, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            return authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.GET, entity, String.class
            );
        } catch (HttpClientErrorException e) {
            // Return error response without throwing so callers can handle specific status codes
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy GET to user-service: path={}", path, e);
            throw new RuntimeException("User service unavailable", e);
        }
    }

    private ResponseEntity<String> proxyPostWithAuth(String path, Object body, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            return authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.POST, entity, String.class
            );
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy POST to user-service: path={}", path, e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
