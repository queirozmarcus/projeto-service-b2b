package com.scopeflow.adapter.out.persistence;

import com.scopeflow.adapter.out.persistence.proposal.JpaProposalRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.ProposalScopeJsonMapper;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMemberRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMemberSpringRepository;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceSpringRepository;
import com.scopeflow.adapter.out.persistence.user.JpaUserRepositoryAdapter;
import com.scopeflow.adapter.out.persistence.user.JpaUserSpringRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for I6: @Transactional annotations on adapter classes.
 *
 * Verifies that:
 * - Adapter classes have class-level @Transactional(readOnly = true)
 * - Mutating methods have @Transactional (write transaction)
 * - Read-only annotation prevents lock acquisition for queries
 */
@DisplayName("@Transactional on persistence adapters (I6)")
class TransactionalAdapterTest {

    // ============ I6: Class-level @Transactional(readOnly = true) ============

    @Nested
    @DisplayName("JpaWorkspaceMemberRepositoryAdapter transactional annotations")
    class WorkspaceMemberAdapterTransactionalTests {

        @Test
        @DisplayName("Class has @Transactional(readOnly = true) annotation")
        void class_shouldHaveReadOnlyTransactional() {
            Transactional tx = JpaWorkspaceMemberRepositoryAdapter.class
                    .getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }

        @Test
        @DisplayName("save() method has @Transactional (write transaction)")
        void saveMethod_shouldHaveWriteTransactional() throws NoSuchMethodException {
            Method saveMethod = JpaWorkspaceMemberRepositoryAdapter.class
                    .getMethod("save", com.scopeflow.core.domain.workspace.WorkspaceMember.class);

            Transactional tx = saveMethod.getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }

        @Test
        @DisplayName("delete() method has @Transactional (write transaction)")
        void deleteMethod_shouldHaveWriteTransactional() throws NoSuchMethodException {
            Method deleteMethod = JpaWorkspaceMemberRepositoryAdapter.class
                    .getMethod("delete",
                            com.scopeflow.core.domain.workspace.WorkspaceId.class,
                            com.scopeflow.core.domain.user.UserId.class);

            Transactional tx = deleteMethod.getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }
    }

    @Nested
    @DisplayName("JpaWorkspaceRepositoryAdapter transactional annotations")
    class WorkspaceAdapterTransactionalTests {

        @Test
        @DisplayName("JpaWorkspaceRepositoryAdapter class has @Transactional(readOnly = true)")
        void class_shouldHaveReadOnlyTransactional() {
            Transactional tx = JpaWorkspaceRepositoryAdapter.class
                    .getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }
    }

    @Nested
    @DisplayName("JpaProposalRepositoryAdapter transactional annotations")
    class ProposalAdapterTransactionalTests {

        @Test
        @DisplayName("JpaProposalRepositoryAdapter class has @Transactional(readOnly = true)")
        void class_shouldHaveReadOnlyTransactional() {
            Transactional tx = JpaProposalRepositoryAdapter.class
                    .getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }

        @Test
        @DisplayName("save() method has @Transactional (write transaction)")
        void saveMethod_shouldHaveWriteTransactional() throws NoSuchMethodException {
            Method saveMethod = JpaProposalRepositoryAdapter.class
                    .getMethod("save", com.scopeflow.core.domain.proposal.Proposal.class);

            Transactional tx = saveMethod.getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isFalse();
        }
    }

    @Nested
    @DisplayName("JpaUserRepositoryAdapter transactional annotations")
    class UserAdapterTransactionalTests {

        @Test
        @DisplayName("JpaUserRepositoryAdapter class has @Transactional(readOnly = true)")
        void class_shouldHaveReadOnlyTransactional() {
            Transactional tx = JpaUserRepositoryAdapter.class
                    .getAnnotation(Transactional.class);

            assertThat(tx).isNotNull();
            assertThat(tx.readOnly()).isTrue();
        }
    }
}
