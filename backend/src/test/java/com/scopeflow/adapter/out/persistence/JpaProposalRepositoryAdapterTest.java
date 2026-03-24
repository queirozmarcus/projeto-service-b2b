package com.scopeflow.adapter.out.persistence;

import com.scopeflow.adapter.out.persistence.proposal.*;
import com.scopeflow.core.domain.briefing.BriefingSessionId;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaProposalRepositoryAdapter")
class JpaProposalRepositoryAdapterTest {

    @Mock
    private JpaProposalSpringRepository springRepo;

    @Mock
    private ProposalScopeJsonMapper scopeMapper;

    @InjectMocks
    private JpaProposalRepositoryAdapter adapter;

    private UUID proposalId;
    private UUID workspaceId;
    private UUID clientId;
    private UUID briefingId;
    private JpaProposal jpaProposal;

    @BeforeEach
    void setUp() {
        proposalId = UUID.randomUUID();
        workspaceId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        briefingId = UUID.randomUUID();
        jpaProposal = new JpaProposal(
                proposalId, workspaceId, clientId, briefingId,
                "Test Proposal", "DRAFT", null,
                Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("should return ProposalDraft when found with DRAFT status")
    void findById_shouldReturnProposalDraft() {
        // Given
        given(springRepo.findById(proposalId)).willReturn(Optional.of(jpaProposal));
        given(scopeMapper.fromJson(null)).willReturn(null);

        // When
        Optional<Proposal> result = adapter.findById(ProposalId.of(proposalId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(ProposalDraft.class);
        assertThat(result.get().getProposalName()).isEqualTo("Test Proposal");
        assertThat(result.get().status()).isEqualTo(ProposalStatus.DRAFT);
    }

    @Test
    @DisplayName("should return ProposalPublished when status is PUBLISHED")
    void findById_shouldReturnProposalPublished() {
        // Given
        JpaProposal published = new JpaProposal(
                proposalId, workspaceId, clientId, briefingId,
                "Published Proposal", "PUBLISHED", "{}", Instant.now(), Instant.now()
        );
        given(springRepo.findById(proposalId)).willReturn(Optional.of(published));
        given(scopeMapper.fromJson("{}")).willReturn(null);

        // When
        Optional<Proposal> result = adapter.findById(ProposalId.of(proposalId));

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(ProposalPublished.class);
        assertThat(result.get().status()).isEqualTo(ProposalStatus.PUBLISHED);
    }

    @Test
    @DisplayName("should return empty when proposal not found")
    void findById_shouldReturnEmpty_whenNotFound() {
        // Given
        given(springRepo.findById(any(UUID.class))).willReturn(Optional.empty());

        // When
        Optional<Proposal> result = adapter.findById(ProposalId.generate());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should find proposals by workspace")
    void findByWorkspaceId_shouldReturnListOfProposals() {
        // Given
        given(springRepo.findByWorkspaceId(workspaceId)).willReturn(List.of(jpaProposal));
        given(scopeMapper.fromJson(null)).willReturn(null);

        // When
        List<Proposal> proposals = adapter.findByWorkspaceId(new WorkspaceId(workspaceId));

        // Then
        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).getWorkspaceId().value()).isEqualTo(workspaceId);
    }

    @Test
    @DisplayName("should throw for unknown proposal status")
    void findById_shouldThrow_whenUnknownStatus() {
        // Given
        JpaProposal unknown = new JpaProposal(
                proposalId, workspaceId, clientId, briefingId,
                "Unknown", "UNKNOWN", null, Instant.now(), Instant.now()
        );
        given(springRepo.findById(proposalId)).willReturn(Optional.of(unknown));
        given(scopeMapper.fromJson(null)).willReturn(null);

        // When / Then
        assertThatThrownBy(() -> adapter.findById(ProposalId.of(proposalId)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNKNOWN");
    }
}
