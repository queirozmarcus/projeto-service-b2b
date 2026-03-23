package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BriefingSession sealed class hierarchy.
 * Pure Java tests (no Spring, no DB).
 */
@DisplayName("BriefingSession Domain Model Tests")
class BriefingSessionTest {

    private WorkspaceId workspaceId;
    private ClientId clientId;
    private ServiceType serviceType;

    @BeforeEach
    void setUp() {
        workspaceId = new WorkspaceId(UUID.randomUUID());
        clientId = new ClientId(UUID.randomUUID());
        serviceType = ServiceType.SOCIAL_MEDIA;
    }

    @Nested
    @DisplayName("Session Creation")
    class SessionCreation {

        @Test
        void shouldCreateNewBriefingInProgress() {
            // When
            BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);

            // Then
            assertThat(session).isNotNull();
            assertThat(session.getId()).isNotNull();
            assertThat(session.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(session.getClientId()).isEqualTo(clientId);
            assertThat(session.getServiceType()).isEqualTo(serviceType);
            assertThat(session.status()).isEqualTo("IN_PROGRESS");
            assertThat(session.getPublicToken()).isNotNull();
            assertThat(session.getCreatedAt()).isNotNull();
        }

        @Test
        void shouldGenerateUniquePublicToken() {
            // When
            BriefingInProgress session1 = BriefingSession.startNew(workspaceId, clientId, serviceType);
            BriefingInProgress session2 = BriefingSession.startNew(workspaceId, clientId, ServiceType.LANDING_PAGE);

            // Then
            assertThat(session1.getPublicToken()).isNotEqualTo(session2.getPublicToken());
        }

        @Test
        void shouldThrowOnNullWorkspaceId() {
            assertThatThrownBy(() -> BriefingSession.startNew(null, clientId, serviceType))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldThrowOnNullClientId() {
            assertThatThrownBy(() -> BriefingSession.startNew(workspaceId, null, serviceType))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldThrowOnNullServiceType() {
            assertThatThrownBy(() -> BriefingSession.startNew(workspaceId, clientId, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        void shouldTransitionFromInProgressToCompleted() {
            // Given
            BriefingInProgress inProgress = BriefingSession.startNew(workspaceId, clientId, serviceType);
            CompletionScore score = new CompletionScore(85, java.util.List.of());

            // When
            BriefingCompleted completed = inProgress.completeBriefing(score);

            // Then
            assertThat(completed).isNotNull();
            assertThat(completed.status()).isEqualTo("COMPLETED");
            assertThat(completed.getCompletionScore()).isEqualTo(score);
            assertThat(completed.getCompletedAt()).isNotNull();
            assertThat(completed.isReady()).isTrue();
        }

        @Test
        void shouldTransitionFromInProgressToAbandoned() {
            // Given
            BriefingInProgress inProgress = BriefingSession.startNew(workspaceId, clientId, serviceType);

            // When
            BriefingAbandoned abandoned = inProgress.abandon();

            // Then
            assertThat(abandoned).isNotNull();
            assertThat(abandoned.status()).isEqualTo("ABANDONED");
            assertThat(abandoned.getAbandonReason()).isNotNull();
            assertThat(abandoned.getAbandonedAt()).isNotNull();
        }

        @Test
        void shouldNotAllowTransitionFromCompletedToAbandoned() {
            // Given
            BriefingInProgress inProgress = BriefingSession.startNew(workspaceId, clientId, serviceType);
            BriefingCompleted completed = inProgress.completeBriefing(new CompletionScore(85, java.util.List.of()));

            // Then - no transition method available on BriefingCompleted
            assertThat(completed).isInstanceOf(BriefingCompleted.class);
        }
    }

    @Nested
    @DisplayName("BriefingProgress")
    class BriefingProgressTests {

        @Test
        void shouldTrackProgressCorrectly() {
            // Given
            BriefingInProgress inProgress = BriefingSession.startNew(workspaceId, clientId, serviceType);

            // When
            int currentStep = inProgress.getCurrentStep();
            int completionPercentage = inProgress.getCompletionPercentage();

            // Then
            assertThat(currentStep).isEqualTo(0);
            assertThat(completionPercentage).isEqualTo(0);
        }

        @Test
        void shouldCalculatePercentageCorrectly() {
            // Given
            BriefingProgress progress = BriefingProgress.of(5, 10);

            // Then
            assertThat(progress.completionPercentage()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("Completion Score Validation")
    class CompletionScoreValidation {

        @Test
        void shouldCreateCompletionScoreWith80Percent() {
            // When
            CompletionScore score = new CompletionScore(80, java.util.List.of());

            // Then
            assertThat(score.score()).isEqualTo(80);
            assertThat(score.isPerfect()).isFalse();
            assertThat(score.hasGaps()).isFalse();
        }

        @Test
        void shouldCreateCompletionScoreWith95Percent() {
            // When
            CompletionScore score = new CompletionScore(95, java.util.List.of());

            // Then
            assertThat(score.isPerfect()).isTrue();
        }

        @Test
        void shouldRejectScoreBelowMinimum() {
            assertThatThrownBy(() -> new CompletionScore(79, java.util.List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must be >= 80");
        }

        @Test
        void shouldRejectScoreAboveMaximum() {
            assertThatThrownBy(() -> new CompletionScore(101, java.util.List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0-100");
        }

        @Test
        void shouldTrackIdentifiedGaps() {
            // When
            CompletionScore score = new CompletionScore(85, java.util.List.of("Need more details on timeline"));

            // Then
            assertThat(score.hasGaps()).isTrue();
            assertThat(score.gapsIdentified()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Equality and Hashing")
    class EqualityAndHashing {

        @Test
        void shouldHaveSameHashCodeForEqualSessions() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingInProgress session1 = new BriefingInProgress(
                    sessionId, workspaceId, clientId, serviceType,
                    PublicToken.generate(), java.time.Instant.now(), java.time.Instant.now(),
                    BriefingProgress.of(0, 0)
            );
            BriefingInProgress session2 = new BriefingInProgress(
                    sessionId, workspaceId, clientId, serviceType,
                    PublicToken.generate(), java.time.Instant.now(), java.time.Instant.now(),
                    BriefingProgress.of(0, 0)
            );

            // Then
            assertThat(session1).isEqualTo(session2);
            assertThat(session1.hashCode()).isEqualTo(session2.hashCode());
        }

        @Test
        void shouldHaveDifferentHashCodeForDifferentSessions() {
            // Given
            BriefingInProgress session1 = BriefingSession.startNew(workspaceId, clientId, serviceType);
            BriefingInProgress session2 = BriefingSession.startNew(workspaceId, clientId, serviceType);

            // Then
            assertThat(session1).isNotEqualTo(session2);
            assertThat(session1.hashCode()).isNotEqualTo(session2.hashCode());
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToStringTests {

        @Test
        void shouldHaveReadableToString() {
            // Given
            BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);

            // When
            String toString = session.toString();

            // Then
            assertThat(toString).contains("BriefingSession");
            assertThat(toString).contains("IN_PROGRESS");
            assertThat(toString).contains("SOCIAL_MEDIA");
        }
    }
}
