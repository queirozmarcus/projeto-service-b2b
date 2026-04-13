package com.scopeflow.adapter.in.web.user.dto;

import com.scopeflow.core.domain.workspace.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request to create an invited (INACTIVE) user.
 * Used by workspace invite flow when user does not exist.
 */
public record CreateInvitedUserRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Role is required")
        Role role,

        @NotNull(message = "Invited by user ID is required")
        UUID invitedByUserId
) {}
