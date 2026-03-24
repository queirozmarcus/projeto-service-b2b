package com.scopeflow.adapter.in.web.proposal;

import com.scopeflow.adapter.in.web.proposal.dto.*;
import com.scopeflow.core.domain.proposal.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public approval controller: client-facing endpoints (no JWT required).
 *
 * Path: /api/v1/proposals/{id}/approve
 *
 * Clients receive this link via email after a proposal is published.
 * Token-based access (proposal ID acts as public identifier).
 */
@RestController
@RequestMapping("/proposals")
@Tag(name = "Approvals", description = "Client-facing proposal approval (public)")
public class ApprovalControllerV2 {

    private static final Logger log = LoggerFactory.getLogger(ApprovalControllerV2.class);

    private final ProposalService proposalService;

    public ApprovalControllerV2(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    /**
     * GET /proposals/{id}/approve
     * Get approval page data: proposal details for client review.
     *
     * Public endpoint — validates proposal is PUBLISHED, returns scope summary.
     */
    @GetMapping("/{id}/approve")
    @Operation(summary = "Get proposal approval page (public, client-facing)")
    public ProposalResponse getApprovalPage(
            @PathVariable UUID id,
            @RequestParam(required = false) String token
    ) {
        Proposal proposal = proposalService.findById(ProposalId.of(id))
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + id));

        if (!(proposal instanceof ProposalPublished)) {
            throw new InvalidProposalStateException(
                    "Proposal is not available for approval (status: " + proposal.status() + ")"
            );
        }

        return ProposalResponse.from(proposal);
    }

    /**
     * POST /proposals/{id}/approve
     * Submit client approval. Captures name, email, IP, user-agent.
     *
     * Public endpoint — no authentication required.
     */
    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit proposal approval (public, client-facing)")
    public ApprovalResponse approve(
            @PathVariable UUID id,
            @Valid @RequestBody ApproveProposalRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        proposalService.recordApproval(
                ProposalId.of(id),
                request.approverName(),
                request.approverEmail(),
                ipAddress,
                userAgent
        );

        log.info("Proposal approved: proposalId={}, approver={}, ip={}",
                id, request.approverEmail(), ipAddress);

        return new ApprovalResponse(
                UUID.randomUUID(),
                request.approverName(),
                request.approverEmail(),
                "APPROVED",
                ipAddress,
                java.time.Instant.now()
        );
    }

    // ============ Private helpers ============

    private String extractClientIp(HttpServletRequest request) {
        // Respect X-Forwarded-For header for load balancer scenarios
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
