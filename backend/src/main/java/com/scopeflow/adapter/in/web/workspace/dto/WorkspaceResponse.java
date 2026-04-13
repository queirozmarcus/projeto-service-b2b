package com.scopeflow.adapter.in.web.workspace.dto;

import com.scopeflow.core.domain.workspace.Workspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Workspace response DTO.
 */
public record WorkspaceResponse(
        UUID id,
        UUID ownerId,
        String name,
        String niche,
        String toneSettings,
        String status,
        Instant createdAt,
        List<MemberResponse> members
) {
    public static WorkspaceResponse from(Workspace workspace, List<MemberResponse> members) {
        return new WorkspaceResponse(
                workspace.getId().value(),
                workspace.getOwnerId().value(),
                workspace.getName(),
                workspace.getNiche(),
                workspace.getToneSettings(),
                workspace.status(),
                workspace.getCreatedAt(),
                members
        );
    }

    public static WorkspaceResponse from(Workspace workspace) {
        return from(workspace, List.of());
    }
}
