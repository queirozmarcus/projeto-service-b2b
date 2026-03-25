package com.scopeflow.adapter.in.web.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to update mutable fields of a DRAFT proposal.
 *
 * Only the proposal name is editable via this endpoint.
 * Scope updates go through the dedicated /update-scope endpoint.
 */
public record UpdateProposalRequest(
        @NotBlank(message = "proposalName must not be blank")
        @Size(max = 500, message = "proposalName must not exceed 500 characters")
        String proposalName
) {}
