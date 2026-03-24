package com.scopeflow.adapter.in.web.proposal.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request to initiate an approval workflow.
 */
public record InitiateApprovalRequest(
        @NotEmpty
        List<String> approverEmails
) {}
