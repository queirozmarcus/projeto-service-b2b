package com.scopeflow.user.adapter.in.web.user.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for updating the workspace assigned to a user.
 */
public record UpdateUserWorkspaceRequest(
        @NotNull(message = "workspaceId is required")
        UUID workspaceId
) {}
