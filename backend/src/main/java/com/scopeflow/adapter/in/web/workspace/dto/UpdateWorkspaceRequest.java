package com.scopeflow.adapter.in.web.workspace.dto;

import jakarta.validation.constraints.Size;

/**
 * Request to update an existing workspace.
 */
public record UpdateWorkspaceRequest(
        @Size(max = 255)
        String name,

        @Size(max = 100)
        String niche,

        @Size(max = 1000)
        String toneSettings
) {}
