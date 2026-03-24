package com.scopeflow.adapter.in.web.proposal.dto;

import com.scopeflow.core.domain.proposal.ProposalScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Request to update the scope of a draft proposal.
 */
public record UpdateScopeRequest(
        @NotNull @Valid
        ProposalScope scope
) {}
