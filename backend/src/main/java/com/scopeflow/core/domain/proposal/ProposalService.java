package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * ProposalService: domain service for proposal lifecycle.
 *
 * Invariants:
 * - Can only publish a proposal that has a scope defined
 * - Status transitions: DRAFT → PUBLISHED → APPROVED/REJECTED
 * - Version is created on every scope update and on publish
 */
public class ProposalService {

    private final ProposalRepository proposalRepository;
    private final ProposalVersionRepository versionRepository;
    private final ApprovalWorkflowRepository workflowRepository;

    public ProposalService(
            ProposalRepository proposalRepository,
            ProposalVersionRepository versionRepository,
            ApprovalWorkflowRepository workflowRepository
    ) {
        this.proposalRepository = Objects.requireNonNull(proposalRepository);
        this.versionRepository = Objects.requireNonNull(versionRepository);
        this.workflowRepository = Objects.requireNonNull(workflowRepository);
    }

    /**
     * Create a new draft proposal.
     */
    public ProposalDraft createProposal(
            WorkspaceId workspaceId,
            UUID clientId,
            BriefingSessionId briefingId,
            String proposalName
    ) {
        ProposalDraft draft = Proposal.create(workspaceId, clientId, briefingId, proposalName);
        proposalRepository.save(draft);
        return draft;
    }

    /**
     * Update scope of a draft proposal and create a version snapshot.
     *
     * @throws InvalidProposalStateException if proposal is not DRAFT
     */
    public ProposalDraft updateScope(ProposalId proposalId, ProposalScope scope, UUID updatedBy) {
        Proposal proposal = getOrThrow(proposalId);

        if (!(proposal instanceof ProposalDraft draft)) {
            throw new InvalidProposalStateException(
                    "Can only update scope of DRAFT proposal, current status: " + proposal.status()
            );
        }

        ProposalDraft updated = draft.updateScope(scope);
        proposalRepository.save(updated);

        // Save immutable version snapshot
        ProposalVersion version = ProposalVersion.create(proposalId, scope, updatedBy);
        versionRepository.save(version);

        return updated;
    }

    /**
     * Publish a draft proposal.
     *
     * @throws InvalidProposalStateException if not DRAFT or no scope
     */
    public ProposalPublished publish(ProposalId proposalId) {
        Proposal proposal = getOrThrow(proposalId);

        if (!(proposal instanceof ProposalDraft draft)) {
            throw new InvalidProposalStateException(
                    "Can only publish DRAFT proposal, current status: " + proposal.status()
            );
        }

        ProposalPublished published = draft.publish();
        proposalRepository.save(published);

        return published;
    }

    /**
     * Initiate approval workflow for a published proposal.
     */
    public ApprovalWorkflow initiateApproval(ProposalId proposalId, List<String> approverEmails) {
        Proposal proposal = getOrThrow(proposalId);

        if (!(proposal instanceof ProposalPublished)) {
            throw new InvalidProposalStateException(
                    "Can only initiate approval for PUBLISHED proposal, current status: " + proposal.status()
            );
        }

        ApprovalWorkflow workflow = ApprovalWorkflow.create(proposalId, approverEmails);
        workflowRepository.save(workflow);

        return workflow;
    }

    /**
     * Record client approval.
     */
    public void recordApproval(
            ProposalId proposalId,
            String approverName,
            String approverEmail,
            String ipAddress,
            String userAgent
    ) {
        Proposal proposal = getOrThrow(proposalId);
        if (!(proposal instanceof ProposalPublished published)) {
            throw new InvalidProposalStateException("Proposal is not awaiting approval");
        }

        ApprovalWorkflow workflow = workflowRepository.findByProposalId(proposalId)
                .orElseThrow(() -> new InvalidProposalStateException("No active approval workflow found"));

        // Find the pending approval for this email
        Approval updated = workflow.approvals().stream()
                .filter(a -> a.approverEmail().equalsIgnoreCase(approverEmail))
                .findFirst()
                .map(a -> new Approval(
                        a.id(), workflow.id(), approverName, approverEmail,
                        ApprovalStatus.APPROVED, ipAddress, userAgent,
                        java.time.Instant.now()
                ))
                .orElseGet(() -> new Approval(
                        UUID.randomUUID(), workflow.id(), approverName, approverEmail,
                        ApprovalStatus.APPROVED, ipAddress, userAgent,
                        java.time.Instant.now()
                ));

        // Build updated workflow
        List<Approval> updatedApprovals = workflow.approvals().stream()
                .map(a -> a.approverEmail().equalsIgnoreCase(approverEmail) ? updated : a)
                .toList();

        // Check if all approved
        boolean allApproved = updatedApprovals.stream()
                .allMatch(a -> a.status() == ApprovalStatus.APPROVED);

        ApprovalStatus newStatus = allApproved ? ApprovalStatus.APPROVED : ApprovalStatus.IN_PROGRESS;
        java.time.Instant completedAt = allApproved ? java.time.Instant.now() : null;

        ApprovalWorkflow updatedWorkflow = new ApprovalWorkflow(
                workflow.id(), workflow.proposalId(), newStatus,
                updatedApprovals, workflow.initiatedAt(), completedAt
        );
        workflowRepository.save(updatedWorkflow);

        // Transition proposal to APPROVED if all approvals collected
        if (allApproved) {
            ProposalApproved approvedProposal = published.approve();
            proposalRepository.save(approvedProposal);
        }
    }

    /**
     * Rename a draft proposal.
     *
     * @throws InvalidProposalStateException if proposal is not DRAFT
     * @throws ProposalNotFoundException if proposal not found in workspace
     */
    public ProposalDraft renameProposal(ProposalId proposalId, WorkspaceId workspaceId, String newName) {
        Proposal proposal = proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId)
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + proposalId.value()));

        if (!(proposal instanceof ProposalDraft draft)) {
            throw new InvalidProposalStateException(
                    "Can only rename a DRAFT proposal, current status: " + proposal.status()
            );
        }

        ProposalDraft renamed = draft.rename(newName);
        proposalRepository.save(renamed);
        return renamed;
    }

    /**
     * Soft-delete a proposal.
     * Sets deleted_at = now(); the proposal becomes invisible to all list queries.
     *
     * @throws ProposalNotFoundException if proposal not found in workspace
     */
    public void deleteProposal(ProposalId proposalId, WorkspaceId workspaceId) {
        // Verify proposal exists and belongs to workspace before soft-deleting
        proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId)
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + proposalId.value()));

        proposalRepository.softDelete(proposalId, workspaceId);
    }

    public Optional<Proposal> findById(ProposalId id) {
        return proposalRepository.findById(id);
    }

    public Optional<Proposal> findByIdAndWorkspaceId(ProposalId id, WorkspaceId workspaceId) {
        return proposalRepository.findByIdAndWorkspaceId(id, workspaceId);
    }

    public List<Proposal> findByWorkspace(WorkspaceId workspaceId) {
        return proposalRepository.findByWorkspaceId(workspaceId);
    }

    public List<Proposal> findByWorkspaceAndStatus(WorkspaceId workspaceId, ProposalStatus status) {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        return proposalRepository.findByWorkspaceIdAndStatus(workspaceId, status);
    }

    public List<ProposalVersion> findVersions(ProposalId proposalId) {
        return versionRepository.findByProposalId(proposalId);
    }

    public Optional<ApprovalWorkflow> findWorkflow(ProposalId proposalId) {
        return workflowRepository.findByProposalId(proposalId);
    }

    private Proposal getOrThrow(ProposalId id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + id.value()));
    }
}
