package com.scopeflow.adapter.in.web.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to create a new workspace.
 */
public record CreateWorkspaceRequest(
        @NotBlank @Size(max = 255)
        String name,

        @NotBlank @Size(max = 100)
        String niche,

        @Size(max = 1000)
        String toneSettings
) {}
