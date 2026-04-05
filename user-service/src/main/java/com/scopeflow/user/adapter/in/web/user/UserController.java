package com.scopeflow.user.adapter.in.web.user;

import com.scopeflow.user.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.user.adapter.in.web.user.dto.UserResponse;
import com.scopeflow.user.application.service.UserService;
import com.scopeflow.user.domain.exception.DuplicateEmailException;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User management endpoints.
 *
 * Path: /api/v1/users
 * Supports workspace invite flow: lookup by email, create invited user.
 * All endpoints require authentication (JWT).
 */
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/by-email/{email}")
    @Operation(summary = "Get user by email")
    public UserResponse getByEmail(@PathVariable String email) {
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

        if (userService.getUserByEmail(email).isPresent()) {
            throw new DuplicateEmailException(email);
        }

        UserId invitedByUserId = new UserId(request.invitedByUserId());
        userService.getUserById(invitedByUserId)
                .orElseThrow(() -> new InvalidInvitedByUserException(invitedByUserId));

        validateRole(request.role());

        UserId newUserId = UserId.generate();
        String tempPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        PasswordHash tempHash = new PasswordHash(tempPasswordHash);
        String displayName = extractDisplayNameFromEmail(email.normalized());

        UserInactive invitedUser = User.createInvited(newUserId, email, tempHash, displayName);
        userService.saveInvitedUser(invitedUser);

        log.info("Invited user created: userId={}, email={}, invitedBy={}, role={}",
                newUserId.value(), email.normalized(), request.invitedByUserId(), request.role());

        return UserResponse.from(invitedUser);
    }

    private void validateRole(Role role) {
        if (role == Role.OWNER) {
            throw new InvalidRoleException("Cannot invite user with OWNER role. Use workspace creation instead.");
        }
    }

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
