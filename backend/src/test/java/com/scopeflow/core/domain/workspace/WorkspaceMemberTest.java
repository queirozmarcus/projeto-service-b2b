package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.shared.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for WorkspaceMember sealed class hierarchy.
 * Pure domain tests — zero mocks, zero framework.
 */
@DisplayName("WorkspaceMember")
class WorkspaceMemberTest {

    private static final WorkspaceId WORKSPACE_ID = WorkspaceId.generate();
    private static final UserId USER_ID = UserId.generate();

    // ============ Factory: createActive ============

    @Nested
    @DisplayName("WorkspaceMember.createActive()")
    class CreateActive {

        @Test
        @DisplayName("should create MemberActive with correct role and status")
        void shouldCreateMemberActive_withCorrectRoleAndStatus() {
            // when
            MemberActive member = WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, Role.OWNER);

            // then
            assertThat(member).isNotNull();
            assertThat(member.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
            assertThat(member.getUserId()).isEqualTo(USER_ID);
            assertThat(member.getRole()).isEqualTo(Role.OWNER);
            assertThat(member.status()).isEqualTo("ACTIVE");
            assertThat(member.getJoinedAt()).isNotNull();
            assertThat(member.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create MemberActive with MEMBER role")
        void shouldCreateMemberActive_withMemberRole() {
            MemberActive member = WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, Role.MEMBER);

            assertThat(member.getRole()).isEqualTo(Role.MEMBER);
            assertThat(member.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("should throw NullPointerException when workspaceId is null")
        void shouldThrow_whenWorkspaceIdIsNull() {
            assertThatThrownBy(() -> WorkspaceMember.createActive(null, USER_ID, Role.MEMBER))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("WorkspaceId");
        }

        @Test
        @DisplayName("should throw NullPointerException when userId is null")
        void shouldThrow_whenUserIdIsNull() {
            assertThatThrownBy(() -> WorkspaceMember.createActive(WORKSPACE_ID, null, Role.MEMBER))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("UserId");
        }

        @Test
        @DisplayName("should throw NullPointerException when role is null")
        void shouldThrow_whenRoleIsNull() {
            assertThatThrownBy(() -> WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Role");
        }
    }

    // ============ Factory: createInvited ============

    @Nested
    @DisplayName("WorkspaceMember.createInvited()")
    class CreateInvited {

        @Test
        @DisplayName("should create MemberInvited with INVITED status")
        void shouldCreateMemberInvited_withInvitedStatus() {
            // when
            MemberInvited member = WorkspaceMember.createInvited(WORKSPACE_ID, USER_ID, Role.MEMBER);

            // then
            assertThat(member).isNotNull();
            assertThat(member.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
            assertThat(member.getUserId()).isEqualTo(USER_ID);
            assertThat(member.getRole()).isEqualTo(Role.MEMBER);
            assertThat(member.status()).isEqualTo("INVITED");
            assertThat(member.getJoinedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create MemberInvited with ADMIN role")
        void shouldCreateMemberInvited_withAdminRole() {
            MemberInvited member = WorkspaceMember.createInvited(WORKSPACE_ID, USER_ID, Role.ADMIN);

            assertThat(member.getRole()).isEqualTo(Role.ADMIN);
        }
    }

    // ============ MemberLeft state ============

    @Nested
    @DisplayName("MemberLeft")
    class LeftState {

        @Test
        @DisplayName("should have LEFT status")
        void shouldHaveLeftStatus() {
            // given — construct directly (no factory for LEFT state)
            MemberLeft member = new MemberLeft(
                    WORKSPACE_ID, USER_ID, Role.MEMBER,
                    Instant.now(), Instant.now()
            );

            // then
            assertThat(member.status()).isEqualTo("LEFT");
        }

        @Test
        @DisplayName("should preserve all fields in LEFT state")
        void shouldPreserveFields_inLeftState() {
            // given
            Instant joinedAt = Instant.parse("2024-01-01T00:00:00Z");
            Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");

            // when
            MemberLeft member = new MemberLeft(WORKSPACE_ID, USER_ID, Role.ADMIN, joinedAt, updatedAt);

            // then
            assertThat(member.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
            assertThat(member.getUserId()).isEqualTo(USER_ID);
            assertThat(member.getRole()).isEqualTo(Role.ADMIN);
            assertThat(member.getJoinedAt()).isEqualTo(joinedAt);
            assertThat(member.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    // ============ Equality by (workspaceId, userId) ============

    @Nested
    @DisplayName("equals() and hashCode()")
    class Equality {

        @Test
        @DisplayName("should be equal when workspaceId and userId match, regardless of state or role")
        void shouldBeEqual_whenWorkspaceAndUserIdMatch() {
            // given — same pair, different state and role
            MemberActive active = WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, Role.OWNER);
            MemberInvited invited = WorkspaceMember.createInvited(WORKSPACE_ID, USER_ID, Role.MEMBER);

            // then — identity is (workspaceId, userId)
            assertThat(active).isEqualTo(invited);
            assertThat(active.hashCode()).isEqualTo(invited.hashCode());
        }

        @Test
        @DisplayName("should not be equal when userId differs")
        void shouldNotBeEqual_whenUserIdDiffers() {
            // given
            MemberActive member1 = WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, Role.MEMBER);
            MemberActive member2 = WorkspaceMember.createActive(WORKSPACE_ID, UserId.generate(), Role.MEMBER);

            // then
            assertThat(member1).isNotEqualTo(member2);
        }

        @Test
        @DisplayName("should not be equal when workspaceId differs")
        void shouldNotBeEqual_whenWorkspaceIdDiffers() {
            // given
            MemberActive member1 = WorkspaceMember.createActive(WORKSPACE_ID, USER_ID, Role.MEMBER);
            MemberActive member2 = WorkspaceMember.createActive(WorkspaceId.generate(), USER_ID, Role.MEMBER);

            // then
            assertThat(member1).isNotEqualTo(member2);
        }
    }
}
