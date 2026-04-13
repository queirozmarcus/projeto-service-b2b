package com.scopeflow.adapter.in.web.workspace.dto;

import com.scopeflow.core.domain.workspace.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request to invite a user to a workspace.
 */
public record InviteMemberRequest(
        @NotBlank @Email
        String email,

        @NotNull
        Role role
) {}
