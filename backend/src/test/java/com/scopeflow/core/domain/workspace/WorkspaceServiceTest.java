package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.user.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceService")
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @InjectMocks
    private WorkspaceService service;

    private static final UserId OWNER_ID = UserId.generate();
    private static final UserId MEMBER_USER_ID = UserId.generate();
    private static final WorkspaceId WORKSPACE_ID = WorkspaceId.generate();

    // ============ createWorkspace ============

    @Nested
    @DisplayName("createWorkspace")
    class CreateWorkspace {

        @Test
        @DisplayName("should create workspace and save owner as OWNER member when name is unique")
        void shouldCreateWorkspaceAndSaveOwnerMember_whenNameIsUnique() {
            // given
            given(workspaceRepository.existsByName("Acme Agency")).willReturn(false);

            // when
            WorkspaceActive result = service.createWorkspace(OWNER_ID, "Acme Agency", "social-media", "{}");

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.getName()).isEqualTo("Acme Agency");
            assertThat(result.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(result.getNiche()).isEqualTo("social-media");

            verify(workspaceRepository).save(any(WorkspaceActive.class));
            verify(memberRepository).save(any(MemberActive.class));
        }

        @Test
        @DisplayName("should throw WorkspaceNameAlreadyExistsException when name is already taken")
        void shouldThrow_whenNameAlreadyExists() {
            // given
            given(workspaceRepository.existsByName("Duplicate Name")).willReturn(true);

            // when / then
            assertThatThrownBy(() -> service.createWorkspace(OWNER_ID, "Duplicate Name", "landing-page", null))
                    .isInstanceOf(WorkspaceNameAlreadyExistsException.class)
                    .hasMessageContaining("Duplicate Name");

            verify(workspaceRepository, never()).save(any());
            verify(memberRepository, never()).save(any());
        }
    }

    // ============ inviteMember ============

    @Nested
    @DisplayName("inviteMember")
    class InviteMember {

        @Test
        @DisplayName("should save MemberInvited when workspace exists and user is not yet a member")
        void shouldSaveInvitedMember_whenWorkspaceExistsAndUserNotMember() {
            // given
            WorkspaceActive workspace = Workspace.create(WORKSPACE_ID, OWNER_ID, "Acme", "social-media", null);
            given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.empty());

            // when
            service.inviteMember(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER);

            // then
            verify(memberRepository).save(any(MemberInvited.class));
        }

        @Test
        @DisplayName("should throw WorkspaceNotFoundException when workspace does not exist")
        void shouldThrow_whenWorkspaceNotFound() {
            // given
            given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.inviteMember(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER))
                    .isInstanceOf(WorkspaceNotFoundException.class);

            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw MemberAlreadyExistsException when user is already a member")
        void shouldThrow_whenUserAlreadyMember() {
            // given
            WorkspaceActive workspace = Workspace.create(WORKSPACE_ID, OWNER_ID, "Acme", "social-media", null);
            MemberActive existingMember = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER);

            given(workspaceRepository.findById(WORKSPACE_ID)).willReturn(Optional.of(workspace));
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(existingMember));

            // when / then
            assertThatThrownBy(() -> service.inviteMember(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER))
                    .isInstanceOf(MemberAlreadyExistsException.class);

            verify(memberRepository, never()).save(any());
        }
    }

    // ============ updateMemberRole ============

    @Nested
    @DisplayName("updateMemberRole")
    class UpdateMemberRole {

        @Test
        @DisplayName("should update role when target is OWNER and at least one other OWNER exists")
        void shouldUpdateRole_whenOwnerHasOtherOwners() {
            // given — member is OWNER but there are 2 OWNERs total
            MemberActive ownerMember = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(ownerMember));
            given(memberRepository.countOwnersByWorkspace(WORKSPACE_ID)).willReturn(2);

            // when — demote to MEMBER (invariant is satisfied: not last OWNER)
            service.updateMemberRole(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER);

            // then — no exception; invariant check passed
            verify(memberRepository).countOwnersByWorkspace(WORKSPACE_ID);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when demoting the only OWNER")
        void shouldThrow_whenDemotingLastOwner() {
            // given — member is the sole OWNER
            MemberActive lastOwner = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(lastOwner));
            given(memberRepository.countOwnersByWorkspace(WORKSPACE_ID)).willReturn(1);

            // when / then
            assertThatThrownBy(() -> service.updateMemberRole(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER))
                    .isInstanceOf(CannotRemoveLastOwnerException.class)
                    .hasMessageContaining("OWNER");
        }

        @Test
        @DisplayName("should not check owner count when target role is also OWNER")
        void shouldNotCheckOwnerCount_whenNewRoleIsAlsoOwner() {
            // given — member is OWNER; new role is also OWNER (no-op on invariant)
            MemberActive ownerMember = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(ownerMember));

            // when
            service.updateMemberRole(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);

            // then — countOwners was never called because role is not changing away from OWNER
            verify(memberRepository, never()).countOwnersByWorkspace(any());
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrow_whenMemberNotFound() {
            // given
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.updateMemberRole(WORKSPACE_ID, MEMBER_USER_ID, Role.ADMIN))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    // ============ removeMember ============

    @Nested
    @DisplayName("removeMember")
    class RemoveMember {

        @Test
        @DisplayName("should delete member when member is not an OWNER")
        void shouldDeleteMember_whenMemberIsNotOwner() {
            // given
            MemberActive regularMember = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.MEMBER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(regularMember));

            // when
            service.removeMember(WORKSPACE_ID, MEMBER_USER_ID);

            // then
            verify(memberRepository).delete(WORKSPACE_ID, MEMBER_USER_ID);
        }

        @Test
        @DisplayName("should delete OWNER member when at least one other OWNER exists")
        void shouldDeleteOwnerMember_whenOtherOwnersExist() {
            // given — member is OWNER but there are 2 OWNERs
            MemberActive ownerMember = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(ownerMember));
            given(memberRepository.countOwnersByWorkspace(WORKSPACE_ID)).willReturn(2);

            // when
            service.removeMember(WORKSPACE_ID, MEMBER_USER_ID);

            // then
            verify(memberRepository).delete(WORKSPACE_ID, MEMBER_USER_ID);
        }

        @Test
        @DisplayName("should throw CannotRemoveLastOwnerException when removing the only OWNER")
        void shouldThrow_whenRemovingLastOwner() {
            // given
            MemberActive lastOwner = WorkspaceMember.createActive(WORKSPACE_ID, MEMBER_USER_ID, Role.OWNER);
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.of(lastOwner));
            given(memberRepository.countOwnersByWorkspace(WORKSPACE_ID)).willReturn(1);

            // when / then
            assertThatThrownBy(() -> service.removeMember(WORKSPACE_ID, MEMBER_USER_ID))
                    .isInstanceOf(CannotRemoveLastOwnerException.class)
                    .hasMessageContaining("OWNER");

            verify(memberRepository, never()).delete(any(), any());
        }

        @Test
        @DisplayName("should throw MemberNotFoundException when member does not exist")
        void shouldThrow_whenMemberNotFound() {
            // given
            given(memberRepository.findByWorkspaceAndUser(WORKSPACE_ID, MEMBER_USER_ID))
                    .willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.removeMember(WORKSPACE_ID, MEMBER_USER_ID))
                    .isInstanceOf(MemberNotFoundException.class);

            verify(memberRepository, never()).delete(any(), any());
        }
    }
}
