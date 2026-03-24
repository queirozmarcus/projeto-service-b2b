package com.scopeflow.adapter.out.persistence;

import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMember;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMemberRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMemberSpringRepository;
import com.scopeflow.core.domain.user.UserId;
import com.scopeflow.core.domain.workspace.*;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("JpaWorkspaceMemberRepositoryAdapter")
class JpaWorkspaceMemberRepositoryAdapterTest {

    @Mock
    private JpaWorkspaceMemberSpringRepository springRepo;

    @InjectMocks
    private JpaWorkspaceMemberRepositoryAdapter adapter;

    private UUID workspaceId;
    private UUID userId;
    private JpaWorkspaceMember jpaActiveMember;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        userId = UUID.randomUUID();
        jpaActiveMember = new JpaWorkspaceMember(
                UUID.randomUUID(), workspaceId, userId,
                "OWNER", "ACTIVE", Instant.now(), Instant.now()
        );
    }

    @Test
    @DisplayName("should return MemberActive when found with ACTIVE status")
    void findByWorkspaceAndUser_shouldReturnMemberActive() {
        // Given
        given(springRepo.findByWorkspaceIdAndUserId(workspaceId, userId))
                .willReturn(Optional.of(jpaActiveMember));

        // When
        Optional<WorkspaceMember> result = adapter.findByWorkspaceAndUser(
                new WorkspaceId(workspaceId), new UserId(userId)
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(MemberActive.class);
        assertThat(result.get().getRole()).isEqualTo(Role.OWNER);
        assertThat(result.get().status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("should return MemberInvited when status is INVITED")
    void findByWorkspaceAndUser_shouldReturnMemberInvited() {
        // Given
        JpaWorkspaceMember invited = new JpaWorkspaceMember(
                UUID.randomUUID(), workspaceId, userId,
                "MEMBER", "INVITED", Instant.now(), Instant.now()
        );
        given(springRepo.findByWorkspaceIdAndUserId(workspaceId, userId))
                .willReturn(Optional.of(invited));

        // When
        Optional<WorkspaceMember> result = adapter.findByWorkspaceAndUser(
                new WorkspaceId(workspaceId), new UserId(userId)
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(MemberInvited.class);
        assertThat(result.get().getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("should count active owners by delegating to spring repo")
    void countOwnersByWorkspace_shouldReturnCorrectCount() {
        // Given
        given(springRepo.countActiveOwners(workspaceId)).willReturn(2);

        // When
        int count = adapter.countOwnersByWorkspace(new WorkspaceId(workspaceId));

        // Then
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should return all members for workspace")
    void findAllByWorkspace_shouldReturnAllMembers() {
        // Given
        UUID userId2 = UUID.randomUUID();
        JpaWorkspaceMember member2 = new JpaWorkspaceMember(
                UUID.randomUUID(), workspaceId, userId2,
                "ADMIN", "ACTIVE", Instant.now(), Instant.now()
        );
        given(springRepo.findByWorkspaceId(workspaceId))
                .willReturn(List.of(jpaActiveMember, member2));

        // When
        List<WorkspaceMember> members = adapter.findAllByWorkspace(new WorkspaceId(workspaceId));

        // Then
        assertThat(members).hasSize(2);
        assertThat(members).allMatch(m -> m.getWorkspaceId().value().equals(workspaceId));
    }

    @Test
    @DisplayName("should soft-delete member by setting status to LEFT")
    void delete_shouldSetStatusToLeft() {
        // Given
        given(springRepo.findByWorkspaceIdAndUserId(workspaceId, userId))
                .willReturn(Optional.of(jpaActiveMember));
        given(springRepo.save(any())).willReturn(jpaActiveMember);

        // When
        adapter.delete(new WorkspaceId(workspaceId), new UserId(userId));

        // Then
        then(springRepo).should().save(any(JpaWorkspaceMember.class));
        assertThat(jpaActiveMember.getStatus()).isEqualTo("LEFT");
    }
}
