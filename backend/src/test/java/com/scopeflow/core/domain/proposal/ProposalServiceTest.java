package com.scopeflow.core.domain.proposal;

import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProposalService")
class ProposalServiceTest {

    @Mock
    private ProposalRepository proposalRepository;

    @Mock
    private ProposalVersionRepository versionRepository;

    @Mock
    private ApprovalWorkflowRepository workflowRepository;

    @InjectMocks
    private ProposalService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CLIENT_ID    = UUID.randomUUID();
    private static final UUID BRIEFING_ID  = UUID.randomUUID();

    private WorkspaceId workspaceId;
    private ProposalId proposalId;
    private ProposalDraft draft;

    @BeforeEach
    void setUp() {
        workspaceId = new WorkspaceId(WORKSPACE_ID);
        proposalId = ProposalId.generate();
        draft = new ProposalDraft(
                proposalId,
                workspaceId,
                CLIENT_ID,
                new BriefingSessionId(BRIEFING_ID),
                "Original Title",
                null,
                Instant.now(),
                Instant.now()
        );
    }

    // ============ renameProposal ============

    @Nested
    @DisplayName("renameProposal")
    class RenameProposal {

        @Test
        @DisplayName("should rename a DRAFT proposal and persist it")
        void shouldRenameAndSave_whenDraft() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(draft));

            // When
            ProposalDraft result = service.renameProposal(proposalId, workspaceId, "New Title");

            // Then
            assertThat(result.getProposalName()).isEqualTo("New Title");
            assertThat(result.status()).isEqualTo(ProposalStatus.DRAFT);
            verify(proposalRepository).save(any(ProposalDraft.class));
        }

        @Test
        @DisplayName("should throw ProposalNotFoundException when proposal not found")
        void shouldThrowNotFound_whenProposalDoesNotExist() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.renameProposal(proposalId, workspaceId, "New Title"))
                    .isInstanceOf(ProposalNotFoundException.class);

            verify(proposalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when not DRAFT")
        void shouldThrowInvalidState_whenNotDraft() {
            // Given — PUBLISHED proposal
            ProposalPublished published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Original Title",
                    new ProposalScope(List.of(), List.of(), List.of(), null, null),
                    Instant.now(), Instant.now()
            );
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(published));

            // When / Then
            assertThatThrownBy(() -> service.renameProposal(proposalId, workspaceId, "New Title"))
                    .isInstanceOf(InvalidProposalStateException.class)
                    .hasMessageContaining("DRAFT");

            verify(proposalRepository, never()).save(any());
        }

        @Test
        @DisplayName("should strip whitespace from new name")
        void shouldStripWhitespace_fromNewName() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(draft));

            // When
            ProposalDraft result = service.renameProposal(proposalId, workspaceId, "  Trimmed Title  ");

            // Then
            assertThat(result.getProposalName()).isEqualTo("Trimmed Title");
        }
    }

    // ============ deleteProposal ============

    @Nested
    @DisplayName("deleteProposal")
    class DeleteProposal {

        @Test
        @DisplayName("should soft-delete proposal when it exists in the workspace")
        void shouldSoftDelete_whenProposalExists() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(draft));

            // When
            service.deleteProposal(proposalId, workspaceId);

            // Then
            verify(proposalRepository).softDelete(proposalId, workspaceId);
        }

        @Test
        @DisplayName("should throw ProposalNotFoundException when proposal not found")
        void shouldThrowNotFound_whenProposalDoesNotExist() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.deleteProposal(proposalId, workspaceId))
                    .isInstanceOf(ProposalNotFoundException.class);

            verify(proposalRepository, never()).softDelete(any(), any());
        }

        @Test
        @DisplayName("should allow soft-deleting a PUBLISHED proposal")
        void shouldAllowDelete_ofPublishedProposal() {
            // Given — published proposals can also be deleted
            ProposalPublished published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Published Proposal",
                    new ProposalScope(List.of(), List.of(), List.of(), null, null),
                    Instant.now(), Instant.now()
            );
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(published));

            // When
            service.deleteProposal(proposalId, workspaceId);

            // Then
            verify(proposalRepository).softDelete(proposalId, workspaceId);
        }
    }

    // ============ findByIdAndWorkspaceId ============

    @Nested
    @DisplayName("findByIdAndWorkspaceId")
    class FindByIdAndWorkspaceId {

        @Test
        @DisplayName("should return proposal when found")
        void shouldReturnProposal_whenFound() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.of(draft));

            // When
            Optional<Proposal> result = service.findByIdAndWorkspaceId(proposalId, workspaceId);

            // Then
            assertThat(result).isPresent()
                    .get().isEqualTo(draft);
        }

        @Test
        @DisplayName("should return empty when not found in workspace")
        void shouldReturnEmpty_whenNotFound() {
            // Given
            given(proposalRepository.findByIdAndWorkspaceId(proposalId, workspaceId))
                    .willReturn(Optional.empty());

            // When
            Optional<Proposal> result = service.findByIdAndWorkspaceId(proposalId, workspaceId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ============ createProposal ============

    @Nested
    @DisplayName("createProposal")
    class CreateProposal {

        @Test
        @DisplayName("should create ProposalDraft and persist it")
        void shouldCreateDraftAndSave() {
            // Given — no stubs needed; createProposal has no preconditions

            // When
            ProposalDraft result = service.createProposal(
                    workspaceId, CLIENT_ID, new BriefingSessionId(BRIEFING_ID), "My Proposal"
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ProposalStatus.DRAFT);
            assertThat(result.getProposalName()).isEqualTo("My Proposal");
            assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(result.getScope()).isNull(); // fresh draft has no scope

            verify(proposalRepository).save(any(ProposalDraft.class));
        }
    }

    // ============ updateScope ============

    @Nested
    @DisplayName("updateScope")
    class UpdateScope {

        private static final ProposalScope SCOPE =
                new ProposalScope(List.of(), List.of(), List.of(), null, null);

        @Test
        @DisplayName("should update scope, save updated draft and save version snapshot")
        void shouldUpdateScopeAndSaveVersion_whenDraft() {
            // Given
            UUID updatedBy = UUID.randomUUID();
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(draft));

            // When
            ProposalDraft result = service.updateScope(proposalId, SCOPE, updatedBy);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getScope()).isEqualTo(SCOPE);
            assertThat(result.status()).isEqualTo(ProposalStatus.DRAFT);

            verify(proposalRepository).save(any(ProposalDraft.class));
            verify(versionRepository).save(any(ProposalVersion.class));
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when proposal is not DRAFT")
        void shouldThrow_whenNotDraft() {
            // Given — PUBLISHED proposal
            UUID updatedBy = UUID.randomUUID();
            ProposalPublished published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Published", SCOPE, Instant.now(), Instant.now()
            );
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));

            // When / Then
            assertThatThrownBy(() -> service.updateScope(proposalId, SCOPE, updatedBy))
                    .isInstanceOf(InvalidProposalStateException.class)
                    .hasMessageContaining("DRAFT");

            verify(proposalRepository, never()).save(any());
            verify(versionRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ProposalNotFoundException when proposal does not exist")
        void shouldThrow_whenProposalNotFound() {
            // Given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.updateScope(proposalId, SCOPE, UUID.randomUUID()))
                    .isInstanceOf(ProposalNotFoundException.class);
        }
    }

    // ============ publish ============

    @Nested
    @DisplayName("publish")
    class Publish {

        private static final ProposalScope SCOPE =
                new ProposalScope(List.of(), List.of(), List.of(), null, null);

        @Test
        @DisplayName("should transition DRAFT to PUBLISHED when draft has a scope")
        void shouldTransitionToPublished_whenDraftHasScope() {
            // Given — draft with scope set
            ProposalDraft draftWithScope = new ProposalDraft(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Ready Proposal", SCOPE, Instant.now(), Instant.now()
            );
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(draftWithScope));

            // When
            ProposalPublished result = service.publish(proposalId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(ProposalStatus.PUBLISHED);
            assertThat(result.getId()).isEqualTo(proposalId);

            verify(proposalRepository).save(any(ProposalPublished.class));
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when proposal is already PUBLISHED")
        void shouldThrow_whenAlreadyPublished() {
            // Given
            ProposalPublished published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Published", SCOPE, Instant.now(), Instant.now()
            );
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));

            // When / Then
            assertThatThrownBy(() -> service.publish(proposalId))
                    .isInstanceOf(InvalidProposalStateException.class)
                    .hasMessageContaining("DRAFT");

            verify(proposalRepository, never()).save(any(ProposalPublished.class));
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when DRAFT has no scope")
        void shouldThrow_whenDraftHasNoScope() {
            // Given — draft without scope (null scope)
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(draft));

            // When / Then — ProposalDraft.publish() throws when scope == null
            assertThatThrownBy(() -> service.publish(proposalId))
                    .isInstanceOf(InvalidProposalStateException.class);
        }
    }

    // ============ initiateApproval ============

    @Nested
    @DisplayName("initiateApproval")
    class InitiateApproval {

        private static final ProposalScope SCOPE =
                new ProposalScope(List.of(), List.of(), List.of(), null, null);

        @Test
        @DisplayName("should create ApprovalWorkflow and persist it when proposal is PUBLISHED")
        void shouldCreateWorkflow_whenPublished() {
            // Given
            ProposalPublished published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Published", SCOPE, Instant.now(), Instant.now()
            );
            List<String> approverEmails = List.of("alice@example.com", "bob@example.com");
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));

            // When
            ApprovalWorkflow workflow = service.initiateApproval(proposalId, approverEmails);

            // Then
            assertThat(workflow).isNotNull();
            assertThat(workflow.proposalId()).isEqualTo(proposalId);
            assertThat(workflow.status()).isEqualTo(ApprovalStatus.IN_PROGRESS);
            assertThat(workflow.approvals()).hasSize(2);
            assertThat(workflow.approvals()).allMatch(a -> a.status() == ApprovalStatus.PENDING);

            verify(workflowRepository).save(any(ApprovalWorkflow.class));
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when proposal is not PUBLISHED")
        void shouldThrow_whenNotPublished() {
            // Given — DRAFT proposal
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(draft));

            // When / Then
            assertThatThrownBy(() -> service.initiateApproval(proposalId, List.of("approver@example.com")))
                    .isInstanceOf(InvalidProposalStateException.class)
                    .hasMessageContaining("PUBLISHED");

            verify(workflowRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw ProposalNotFoundException when proposal does not exist")
        void shouldThrow_whenProposalNotFound() {
            // Given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> service.initiateApproval(proposalId, List.of("approver@example.com")))
                    .isInstanceOf(ProposalNotFoundException.class);
        }
    }

    // ============ recordApproval ============

    @Nested
    @DisplayName("recordApproval")
    class RecordApproval {

        private static final ProposalScope SCOPE =
                new ProposalScope(List.of(), List.of(), List.of(), null, null);
        private static final String ALICE_EMAIL = "alice@example.com";
        private static final String BOB_EMAIL   = "bob@example.com";

        private ProposalPublished published;
        private ApprovalWorkflow workflowWithTwoApprovers;

        @BeforeEach
        void setUpApprovalContext() {
            published = new ProposalPublished(
                    proposalId, workspaceId, CLIENT_ID,
                    new BriefingSessionId(BRIEFING_ID),
                    "Published Proposal", SCOPE, Instant.now(), Instant.now()
            );

            // ApprovalWorkflow with two PENDING approvals
            workflowWithTwoApprovers = ApprovalWorkflow.create(proposalId, List.of(ALICE_EMAIL, BOB_EMAIL));
        }

        @Test
        @DisplayName("should record partial approval and keep workflow IN_PROGRESS when not all approvers have acted")
        void shouldKeepInProgress_whenOnlyOneOfTwoApproversApproved() {
            // Given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));
            given(workflowRepository.findByProposalId(proposalId))
                    .willReturn(Optional.of(workflowWithTwoApprovers));

            // When — only ALICE approves
            service.recordApproval(proposalId, "Alice", ALICE_EMAIL, "10.0.0.1", "Mozilla/5.0");

            // Then — workflow is updated but proposal stays PUBLISHED (not all approved yet)
            verify(workflowRepository).save(any(ApprovalWorkflow.class));
            // proposalRepository.save is NOT called because approval is still partial
            verify(proposalRepository, never()).save(any(ProposalApproved.class));
        }

        @Test
        @DisplayName("should transition proposal to APPROVED and complete workflow when all approvers approve")
        void shouldApproveProposal_whenAllApproversApprove() {
            // Given — build a workflow where ALICE already approved (only BOB is pending)
            UUID aliceApprovalId = UUID.randomUUID();
            UUID bobApprovalId   = UUID.randomUUID();
            ApprovalWorkflowId workflowId = ApprovalWorkflowId.generate();

            Approval aliceApproved = new Approval(
                    aliceApprovalId, workflowId, "Alice", ALICE_EMAIL,
                    ApprovalStatus.APPROVED, "10.0.0.1", "Mozilla/5.0", Instant.now()
            );
            Approval bobPending = new Approval(
                    bobApprovalId, workflowId, null, BOB_EMAIL,
                    ApprovalStatus.PENDING, null, null, null
            );

            ApprovalWorkflow workflowAliceApproved = new ApprovalWorkflow(
                    workflowId, proposalId, ApprovalStatus.IN_PROGRESS,
                    List.of(aliceApproved, bobPending), Instant.now(), null
            );

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));
            given(workflowRepository.findByProposalId(proposalId))
                    .willReturn(Optional.of(workflowAliceApproved));

            // When — BOB approves (all approvers done)
            service.recordApproval(proposalId, "Bob", BOB_EMAIL, "10.0.0.2", "Chrome/120");

            // Then — workflow saved as APPROVED and proposal transitioned to ProposalApproved
            verify(workflowRepository).save(any(ApprovalWorkflow.class));
            verify(proposalRepository).save(any(ProposalApproved.class));
        }

        @Test
        @DisplayName("should add approval for email not in original workflow list")
        void shouldAddNewApproval_whenEmailNotInOriginalList() {
            // Given — workflow with only ALICE; BOB sends approval without being in the list
            ApprovalWorkflow workflowAliceOnly = ApprovalWorkflow.create(proposalId, List.of(ALICE_EMAIL));

            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));
            given(workflowRepository.findByProposalId(proposalId))
                    .willReturn(Optional.of(workflowAliceOnly));

            // When — BOB (not in list) sends approval; service creates a new Approval record for him
            service.recordApproval(proposalId, "Bob", BOB_EMAIL, "10.0.0.2", "Chrome/120");

            // Then — workflow is updated; ALICE is still pending so proposal stays PUBLISHED
            verify(workflowRepository).save(any(ApprovalWorkflow.class));
            verify(proposalRepository, never()).save(any(ProposalApproved.class));
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when proposal is not PUBLISHED")
        void shouldThrow_whenProposalNotPublished() {
            // Given — DRAFT proposal
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(draft));

            // When / Then
            assertThatThrownBy(() ->
                    service.recordApproval(proposalId, "Alice", ALICE_EMAIL, "10.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(InvalidProposalStateException.class);

            verify(workflowRepository, never()).findByProposalId(any());
        }

        @Test
        @DisplayName("should throw InvalidProposalStateException when no active workflow exists")
        void shouldThrow_whenNoWorkflowFound() {
            // Given
            given(proposalRepository.findById(proposalId)).willReturn(Optional.of(published));
            given(workflowRepository.findByProposalId(proposalId)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() ->
                    service.recordApproval(proposalId, "Alice", ALICE_EMAIL, "10.0.0.1", "Mozilla/5.0"))
                    .isInstanceOf(InvalidProposalStateException.class)
                    .hasMessageContaining("workflow");
        }
    }
}
