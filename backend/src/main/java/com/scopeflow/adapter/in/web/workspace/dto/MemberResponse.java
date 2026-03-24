package com.scopeflow.adapter.in.web.workspace.dto;

import com.scopeflow.core.domain.workspace.WorkspaceMember;

import java.time.Instant;
import java.util.UUID;

/**
 * Workspace member response DTO.
 */
public record MemberResponse(
        UUID userId,
        String role,
        String status,
        Instant joinedAt
) {
    public static MemberResponse from(WorkspaceMember member) {
        return new MemberResponse(
                member.getUserId().value(),
                member.getRole().name(),
                member.status(),
                member.getJoinedAt()
        );
    }
}
