package com.scopeflow.user.adapter.in.web.user;

import com.scopeflow.user.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.user.adapter.in.web.user.dto.UpdateUserWorkspaceRequest;
import com.scopeflow.user.adapter.in.web.user.dto.UserProfileResponse;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.application.usecase.BlockUserByIdUseCase;
import com.scopeflow.user.application.usecase.InviteUserUseCase;
import com.scopeflow.user.application.usecase.UpdateUserWorkspaceUseCase;
import com.scopeflow.user.config.InternalTokenProperties;
import com.scopeflow.user.config.ScopeFlowPrincipal;
import com.scopeflow.user.domain.exception.InvalidInvitedByUserException;
import com.scopeflow.user.domain.exception.InvalidRoleException;
import com.scopeflow.user.domain.exception.UserNotFoundException;
import com.scopeflow.user.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management endpoints.
 *
 * Path: /api/v1/users
 * Supports workspace invite flow: lookup by email, create invited user.
 * All endpoints require authentication (JWT).
 *
 * Controller responsibility: HTTP translation only (validate input, call use case, build response).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final InviteUserUseCase inviteUserUseCase;
    private final BlockUserByIdUseCase blockUserByIdUseCase;
    private final UpdateUserWorkspaceUseCase updateUserWorkspaceUseCase;
    private final InternalTokenProperties internalTokenProperties;

    public UserController(
            UserService userService,
            InviteUserUseCase inviteUserUseCase,
            BlockUserByIdUseCase blockUserByIdUseCase,
            UpdateUserWorkspaceUseCase updateUserWorkspaceUseCase,
            InternalTokenProperties internalTokenProperties
    ) {
        this.userService = userService;
        this.inviteUserUseCase = inviteUserUseCase;
        this.blockUserByIdUseCase = blockUserByIdUseCase;
        this.updateUserWorkspaceUseCase = updateUserWorkspaceUseCase;
        this.internalTokenProperties = internalTokenProperties;
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get user by email")
    public UserProfileResponse getByEmail(@RequestParam(required = true) String email) {
        Email emailVO = new Email(email);
        User user = userService.getUserByEmail(emailVO)
                .orElseThrow(() -> new UserNotFoundException(emailVO));

        log.info("User found by email: userId={}, email={}", user.getId().value(), email);
        return UserProfileResponse.from(user);
    }

    @PostMapping("/invited")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create invited user")
    public UserProfileResponse createInvited(@Valid @RequestBody CreateInvitedUserRequest request) {
        Email email = new Email(request.email());

        UserId invitedByUserId = new UserId(request.invitedByUserId());
        userService.getUserById(invitedByUserId)
                .orElseThrow(() -> new InvalidInvitedByUserException(invitedByUserId));

        if (request.role() == Role.OWNER) {
            throw new InvalidRoleException("Cannot invite user with OWNER role. Use workspace creation instead.");
        }

        UserInactive invitedUser = inviteUserUseCase.execute(email, invitedByUserId);

        log.info("Invited user created: userId={}, email={}, invitedBy={}, role={}",
                invitedUser.getId().value(), email.normalized(), request.invitedByUserId(), request.role());

        return UserProfileResponse.from(invitedUser);
    }

    @PatchMapping("/{userId}/workspace")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Assign workspace to user")
    public void updateWorkspace(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserWorkspaceRequest request,
            @AuthenticationPrincipal ScopeFlowPrincipal principal,
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken) {

        boolean isInternalCall = internalTokenProperties.getInternalToken().equals(internalToken);
        boolean isOwner = principal != null && userId.equals(principal.userId());

        if (!isInternalCall && !isOwner) {
            log.warn("Unauthorized workspace assignment attempt: requestedUserId={}, authenticatedUserId={}",
                    userId, principal != null ? principal.userId() : "none");
            throw new AccessDeniedException("Access denied: not the owner of this user resource");
        }

        updateUserWorkspaceUseCase.execute(new UserId(userId), request.workspaceId());

        log.info("Workspace assigned: userId={}, workspaceId={}, via={}",
                userId, request.workspaceId(), isInternalCall ? "internal-token" : "jwt-owner");
    }

    @PostMapping("/{id}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block user by ID (admin only)")
    public void blockUser(@PathVariable UUID id) {
        UserId userId = new UserId(id);
        blockUserByIdUseCase.execute(userId);

        log.info("User blocked: userId={}", id);
    }
}
