package com.scopeflow.adapter.in.web.workspace.dto;

import com.scopeflow.core.domain.workspace.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request to change a member's role.
 */
public record UpdateRoleRequest(
        @NotNull
        Role role
) {}
