package com.scopeflow.application;

import com.scopeflow.application.port.out.BriefingRepositoryPort;
import com.scopeflow.application.port.out.ProposalRepositoryPort;
import com.scopeflow.application.port.out.ScopeGenerationPort;
import com.scopeflow.core.domain.briefing.BriefingCompleted;
import com.scopeflow.core.domain.briefing.BriefingIncompleteException;
import com.scopeflow.core.domain.briefing.BriefingNotReadyException;
import com.scopeflow.core.domain.briefing.BriefingNotFoundException;
import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.proposal.InvalidProposalStateException;
import com.scopeflow.core.domain.proposal.ProposalDraft;
import com.scopeflow.core.domain.proposal.ProposalId;
import com.scopeflow.core.domain.proposal.ProposalNotFoundException;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Use case for AI-powered scope generation.
 *
 * <p>Validates proposal state, briefing completeness, and delegates to AI port.
 * All business rules from Sprint 1 architect decision are enforced here.
 */
@Service
public class GenerateScopeAIUseCase {

    private static final int MIN_COMPLETENESS_PERCENT = 80;

    private final ProposalRepositoryPort proposalRepository;
    private final BriefingRepositoryPort briefingRepository;
    private final ScopeGenerationPort scopeGenerationPort;
    private final ProposalService proposalService;

    public GenerateScopeAIUseCase(
            ProposalRepositoryPort proposalRepository,
            BriefingRepositoryPort briefingRepository,
            ScopeGenerationPort scopeGenerationPort,
            ProposalService proposalService) {
        this.proposalRepository = proposalRepository;
        this.briefingRepository = briefingRepository;
        this.scopeGenerationPort = scopeGenerationPort;
        this.proposalService = proposalService;
    }

    /**
     * Generate AI-powered scope for a proposal.
     *
     * <p>Validation flow:
     * <ol>
     *   <li>Proposal exists and belongs to workspace (PROPOSAL-001)</li>
     *   <li>Proposal is in DRAFT state (PROPOSAL-011)</li>
     *   <li>Linked briefing exists (PROPOSAL-010)</li>
     *   <li>Briefing is COMPLETED (PROPOSAL-012)</li>
     *   <li>Briefing completeness ≥ 80% (PROPOSAL-013)</li>
     * </ol>
     *
     * @param proposalId ID of the proposal to update
     * @param workspaceId Workspace context for authorization
     * @param userId User requesting the generation (for audit)
     * @return Updated proposal draft with AI-generated scope
     * @throws ProposalNotFoundException if proposal not found or wrong workspace
     * @throws InvalidProposalStateException if proposal not in DRAFT state
     * @throws BriefingNotFoundException if briefing not linked
     * @throws BriefingIncompleteException if briefing not COMPLETED
     * @throws BriefingNotReadyException if briefing completeness < 80%
     * @throws ScopeGenerationException if AI service fails
     */
    @Transactional
    public ProposalDraft execute(ProposalId proposalId, WorkspaceId workspaceId, UUID userId) {
        // 1. Fetch proposal and validate workspace isolation
        var proposal = proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (!proposal.getWorkspaceId().equals(workspaceId)) {
            throw new ProposalNotFoundException(proposalId);
        }

        // 2. Validate proposal is in DRAFT state
        if (!(proposal instanceof ProposalDraft draft)) {
            throw new InvalidProposalStateException(
                proposalId,
                "Scope generation is only allowed for proposals in DRAFT state"
            );
        }

        // 3. Fetch linked briefing
        BriefingSessionId briefingId = draft.getBriefingId();
        if (briefingId == null) {
            throw new BriefingNotFoundException(
                "Proposal %s has no linked briefing".formatted(proposalId.value())
            );
        }

        var briefingSession = briefingRepository.findById(briefingId)
            .orElseThrow(() -> new BriefingNotFoundException(
                "Briefing %s not found".formatted(briefingId.value())
            ));

        // 4. Validate briefing is COMPLETED
        if (!(briefingSession instanceof BriefingCompleted completedBriefing)) {
            throw new BriefingIncompleteException(briefingId);
        }

        // 5. Validate briefing completeness ≥ 80%
        int completeness = completedBriefing.getCompletenessPercentage();
        if (completeness < MIN_COMPLETENESS_PERCENT) {
            throw new BriefingNotReadyException(briefingId, completeness);
        }

        // 6. Generate scope via AI port (can throw ScopeGenerationException)
        var generatedScope = scopeGenerationPort.generateScope(completedBriefing);

        // 7. Persist scope via ProposalService
        proposalService.updateScope(proposalId, generatedScope, userId);

        // 8. Return updated draft
        return (ProposalDraft) proposalRepository.findById(proposalId)
            .orElseThrow(() -> new ProposalNotFoundException(proposalId));
    }
}
