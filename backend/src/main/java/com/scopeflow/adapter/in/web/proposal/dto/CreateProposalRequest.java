package com.scopeflow.adapter.in.web.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request to create a new proposal from a completed briefing.
 */
public record CreateProposalRequest(
        @NotNull
        UUID clientId,

        @NotNull
        UUID briefingId,

        @NotBlank @Size(max = 500)
        String proposalName
) {}
