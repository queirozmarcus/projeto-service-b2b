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
}
