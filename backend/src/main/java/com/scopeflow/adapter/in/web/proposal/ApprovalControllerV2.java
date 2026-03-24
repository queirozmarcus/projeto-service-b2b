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

    /**
     * Extract real client IP with spoofing mitigation.
     *
     * X-Forwarded-For is only trusted when the immediate caller (remoteAddr) is a known
     * private/loopback address — meaning a trusted proxy/load balancer forwarded the request.
     * If remoteAddr is a public IP, we do NOT trust X-Forwarded-For to prevent spoofing.
     *
     * Format: "X-Forwarded-For: client, proxy1, proxy2" — we take the leftmost IP.
     */
    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only trust X-Forwarded-For when request comes from a trusted proxy (private network or loopback)
        if (isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                // Take the leftmost IP (original client), ignore intermediaries
                String candidate = forwardedFor.split(",")[0].trim();
                // Basic validation: reject obviously malformed values
                if (isValidIpAddress(candidate)) {
                    return candidate;
                }
            }
        }

        return remoteAddr;
    }

    /**
     * Returns true if the IP is from a trusted private/loopback range.
     * Only these origins are allowed to set X-Forwarded-For.
     */
    private boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return ip.equals("127.0.0.1")
                || ip.equals("::1")
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || ip.startsWith("172.16.")
                || ip.startsWith("172.17.")
                || ip.startsWith("172.18.")
                || ip.startsWith("172.19.")
                || ip.startsWith("172.2")
                || ip.startsWith("172.30.")
                || ip.startsWith("172.31.");
    }

    /**
     * Basic IP address validation: accepts IPv4 and IPv6 formats.
     * Rejects injected values like "1.2.3.4, 5.6.7.8" (already split, should be clean).
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isBlank() || ip.length() > 45) {
            return false;
        }
        // Must contain only valid IP characters (digits, dots, colons, hex for IPv6)
        return ip.matches("[0-9a-fA-F.:]+");
    }
}
