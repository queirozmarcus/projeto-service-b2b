package com.scopeflow.adapter.in.web.proposal;

import com.scopeflow.adapter.in.web.proposal.dto.*;
import com.scopeflow.adapter.in.web.security.SecurityUtil;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Proposal management: create, view, update scope, publish, initiate approval.
 *
 * Path: /api/v1/proposals
 * All endpoints require authentication (JWT).
 * Workspace isolation enforced via JWT workspace_id claim.
 */
@RestController
@RequestMapping("/proposals")
@Tag(name = "Proposals", description = "Proposal lifecycle management")
public class ProposalControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(ProposalControllerV2.class);

    private final ProposalService proposalService;

    public ProposalControllerV2(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    /**
     * POST /proposals
     * Create a new draft proposal from a completed briefing.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create new proposal from briefing")
    public ProposalResponse create(@Valid @RequestBody CreateProposalRequest request) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();

        ProposalDraft proposal = proposalService.createProposal(
                new WorkspaceId(workspaceId),
                request.clientId(),
                new com.scopeflow.core.domain.briefing.BriefingSessionId(request.briefingId()),
                request.proposalName()
        );

        log.info("Proposal created: proposalId={}, workspaceId={}", proposal.getId().value(), workspaceId);
        return ProposalResponse.from(proposal);
    }

    /**
     * GET /proposals/{id}
     * Get proposal details.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get proposal by ID")
    public ProposalResponse getById(@PathVariable UUID id) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();

        Proposal proposal = proposalService.findById(ProposalId.of(id))
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + id));

        // Workspace isolation: ensure proposal belongs to authenticated workspace
        if (!proposal.getWorkspaceId().value().equals(workspaceId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Proposal does not belong to your workspace"
            );
        }

        return ProposalResponse.from(proposal);
    }

    /**
     * GET /proposals
     * List proposals in current workspace with pagination.
     *
     * @param page Page number (zero-based, default 0)
     * @param size Page size (default 20, max 100)
     * @param status Optional status filter (DRAFT, PUBLISHED, APPROVED, REJECTED)
     */
    @GetMapping
    @Operation(summary = "List proposals in workspace with pagination")
    public ProposalPageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();

        // Enforce max page size to prevent memory exhaustion
        int effectiveSize = Math.min(Math.max(size, 1), 100);

        List<Proposal> all = (status != null && !status.isBlank())
                ? proposalService.findByWorkspaceAndStatus(new WorkspaceId(workspaceId),
                    ProposalStatus.valueOf(status.toUpperCase()))
                : proposalService.findByWorkspace(new WorkspaceId(workspaceId));

        int totalElements = all.size();
        int totalPages = (int) Math.ceil((double) totalElements / effectiveSize);
        int fromIndex = Math.min(page * effectiveSize, totalElements);
        int toIndex = Math.min(fromIndex + effectiveSize, totalElements);

        List<ProposalResponse> content = all.subList(fromIndex, toIndex)
                .stream()
                .map(ProposalResponse::from)
                .toList();

        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;

        return ProposalPageResponse.of(content, totalElements, totalPages, effectiveSize, page, first, last);
    }

    /**
     * GET /proposals/{id}/versions
     * Get proposal version history.
     *
     * Workspace isolation: proposal must belong to authenticated workspace.
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "Get proposal version history")
    public List<ProposalVersionResponse> getVersions(@PathVariable UUID id) {
        UUID workspaceId = SecurityUtil.getWorkspaceId();

        // Workspace isolation: verify proposal belongs to authenticated workspace before returning versions
        Proposal proposal = proposalService.findById(ProposalId.of(id))
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + id));

        if (!proposal.getWorkspaceId().value().equals(workspaceId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Proposal does not belong to your workspace"
            );
        }

        return proposalService.findVersions(ProposalId.of(id))
                .stream()
                .map(ProposalVersionResponse::from)
                .toList();
    }

    /**
     * POST /proposals/{id}/update-scope
     * Update scope of a draft proposal.
     */
    @PostMapping("/{id}/update-scope")
    @Operation(summary = "Update proposal scope (DRAFT only)")
    public ProposalResponse updateScope(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateScopeRequest request
    ) {
        UUID userId = SecurityUtil.getUserId();

        ProposalDraft updated = proposalService.updateScope(
                ProposalId.of(id), request.scope(), userId
        );

        log.info("Proposal scope updated: proposalId={}, userId={}", id, userId);
        return ProposalResponse.from(updated);
    }

    /**
     * POST /proposals/{id}/publish
     * Publish a draft proposal (makes it visible to client via approval link).
     */
    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish proposal (DRAFT → PUBLISHED)")
    public ProposalResponse publish(@PathVariable UUID id) {
        ProposalPublished published = proposalService.publish(ProposalId.of(id));
        log.info("Proposal published: proposalId={}", id);
        return ProposalResponse.from(published);
    }

    /**
     * POST /proposals/{id}/initiate-approval
     * Start an approval workflow, sending links to approver emails.
     */
    @PostMapping("/{id}/initiate-approval")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Initiate approval workflow (PUBLISHED only)")
    public ApprovalWorkflowResponse initiateApproval(
            @PathVariable UUID id,
            @Valid @RequestBody InitiateApprovalRequest request
    ) {
        ApprovalWorkflow workflow = proposalService.initiateApproval(
                ProposalId.of(id), request.approverEmails()
        );

        log.info("Approval workflow initiated: proposalId={}, approvers={}",
                id, request.approverEmails().size());
        return ApprovalWorkflowResponse.from(workflow);
    }
}
