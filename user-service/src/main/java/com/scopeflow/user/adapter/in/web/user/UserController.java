package com.scopeflow.user.adapter.in.web.user;

import com.scopeflow.user.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.user.adapter.in.web.user.dto.UserResponse;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.application.usecase.InviteUserUseCase;
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
import org.springframework.web.bind.annotation.*;

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

    public UserController(UserService userService, InviteUserUseCase inviteUserUseCase) {
        this.userService = userService;
        this.inviteUserUseCase = inviteUserUseCase;
    }

    @GetMapping("/by-email")
    @Operation(summary = "Get user by email")
    public UserResponse getByEmail(@RequestParam(required = true) String email) {
        Email emailVO = new Email(email);
        User user = userService.getUserByEmail(emailVO)
                .orElseThrow(() -> new UserNotFoundException(emailVO));

        log.info("User found by email: userId={}, email={}", user.getId().value(), email);
        return UserResponse.from(user);
    }

    @PostMapping("/invited")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create invited user")
    public UserResponse createInvited(@Valid @RequestBody CreateInvitedUserRequest request) {
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

        return UserResponse.from(invitedUser);
    }
}
