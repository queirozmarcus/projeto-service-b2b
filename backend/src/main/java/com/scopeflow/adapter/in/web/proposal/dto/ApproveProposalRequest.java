package com.scopeflow.adapter.in.web.proposal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Client approval request (public endpoint, no JWT).
 */
public record ApproveProposalRequest(
        @NotBlank
        String approverName,

        @NotBlank @Email
        String approverEmail
) {}
