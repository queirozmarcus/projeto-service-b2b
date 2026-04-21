package com.scopeflow.core.domain.workspace;

import com.scopeflow.core.domain.shared.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for Workspace sealed class hierarchy.
 * Pure domain tests — zero mocks, zero framework.
 */
@DisplayName("Workspace")
class WorkspaceTest {

    private static final WorkspaceId ID = WorkspaceId.generate();
    private static final UserId OWNER_ID = UserId.generate();

    // ============ Factory method ============

    @Nested
    @DisplayName("Workspace.create()")
    class Create {

        @Test
        @DisplayName("should create WorkspaceActive with all provided fields")
        void shouldCreateActive_withAllFields() {
            // when
            WorkspaceActive workspace = Workspace.create(ID, OWNER_ID, "Acme Agency", "social-media", "{\"tone\":\"formal\"}");

            // then
            assertThat(workspace).isNotNull();
            assertThat(workspace.getId()).isEqualTo(ID);
            assertThat(workspace.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(workspace.getName()).isEqualTo("Acme Agency");
            assertThat(workspace.getNiche()).isEqualTo("social-media");
            assertThat(workspace.getToneSettings()).isEqualTo("{\"tone\":\"formal\"}");
            assertThat(workspace.getCreatedAt()).isNotNull();
            assertThat(workspace.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("should create WorkspaceActive with null toneSettings (optional field)")
        void shouldCreate_withNullToneSettings() {
            // when
            WorkspaceActive workspace = Workspace.create(ID, OWNER_ID, "No-Tone Workspace", "landing-page", null);

            // then
            assertThat(workspace.getToneSettings()).isNull();
        }

        @Test
        @DisplayName("should return status ACTIVE for WorkspaceActive")
        void shouldReturnActiveStatus() {
            WorkspaceActive workspace = Workspace.create(ID, OWNER_ID, "Agency", "social-media", null);

            assertThat(workspace.status()).isEqualTo("ACTIVE");
            assertThat(workspace).isInstanceOf(WorkspaceActive.class);
        }

        @Test
        @DisplayName("should throw NullPointerException when id is null")
        void shouldThrow_whenIdIsNull() {
            assertThatThrownBy(() -> Workspace.create(null, OWNER_ID, "Agency", "social-media", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("WorkspaceId");
        }

        @Test
        @DisplayName("should throw NullPointerException when ownerId is null")
        void shouldThrow_whenOwnerIdIsNull() {
            assertThatThrownBy(() -> Workspace.create(ID, null, "Agency", "social-media", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("OwnerId");
        }

        @Test
        @DisplayName("should throw NullPointerException when name is null")
        void shouldThrow_whenNameIsNull() {
            assertThatThrownBy(() -> Workspace.create(ID, OWNER_ID, null, "social-media", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Name");
        }

        @Test
        @DisplayName("should throw NullPointerException when niche is null")
        void shouldThrow_whenNicheIsNull() {
            assertThatThrownBy(() -> Workspace.create(ID, OWNER_ID, "Agency", null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("Niche");
        }
    }

    // ============ WorkspaceSuspended state ============

    @Nested
    @DisplayName("WorkspaceSuspended")
    class SuspendedState {

        @Test
        @DisplayName("should return status SUSPENDED for WorkspaceSuspended")
        void shouldReturnSuspendedStatus() {
            // given — construct directly (sealed hierarchy, no factory for suspended yet)
            WorkspaceSuspended suspended = new WorkspaceSuspended(
                    ID, OWNER_ID, "Agency", "social-media", null,
                    Instant.now(), Instant.now()
            );

            // then
            assertThat(suspended.status()).isEqualTo("SUSPENDED");
            assertThat(suspended).isInstanceOf(WorkspaceSuspended.class);
        }

        @Test
        @DisplayName("should preserve all fields in suspended state")
        void shouldPreserveFields_inSuspendedState() {
            // given
            Instant createdAt = Instant.parse("2024-01-01T00:00:00Z");
            Instant updatedAt = Instant.parse("2024-06-01T00:00:00Z");

            // when
            WorkspaceSuspended suspended = new WorkspaceSuspended(
                    ID, OWNER_ID, "Suspended Agency", "landing-page", "{}", createdAt, updatedAt
            );

            // then
            assertThat(suspended.getId()).isEqualTo(ID);
            assertThat(suspended.getOwnerId()).isEqualTo(OWNER_ID);
            assertThat(suspended.getName()).isEqualTo("Suspended Agency");
            assertThat(suspended.getNiche()).isEqualTo("landing-page");
            assertThat(suspended.getCreatedAt()).isEqualTo(createdAt);
            assertThat(suspended.getUpdatedAt()).isEqualTo(updatedAt);
        }
    }

    // ============ Equality ============

    @Nested
    @DisplayName("equals() and hashCode()")
    class Equality {

        @Test
        @DisplayName("should consider two workspaces equal when they share the same id")
        void shouldBeEqual_whenSameId() {
            // given
            WorkspaceActive workspace1 = Workspace.create(ID, OWNER_ID, "Agency A", "social-media", null);
            WorkspaceSuspended workspace2 = new WorkspaceSuspended(
                    ID, UserId.generate(), "Agency B", "landing-page", null, Instant.now(), Instant.now()
            );

            // then — identity is by id only (domain rule)
            assertThat(workspace1).isEqualTo(workspace2);
            assertThat(workspace1.hashCode()).isEqualTo(workspace2.hashCode());
        }

        @Test
        @DisplayName("should consider two workspaces different when ids differ")
        void shouldNotBeEqual_whenDifferentIds() {
            // given
            WorkspaceActive workspace1 = Workspace.create(WorkspaceId.generate(), OWNER_ID, "Agency A", "social-media", null);
            WorkspaceActive workspace2 = Workspace.create(WorkspaceId.generate(), OWNER_ID, "Agency A", "social-media", null);

            // then
            assertThat(workspace1).isNotEqualTo(workspace2);
        }
    }
}
