package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BriefingService domain service.
 */
@DisplayName("BriefingService Domain Service Tests")
class BriefingServiceTest {

    private BriefingService service;
    private BriefingSessionRepository sessionRepository;
    private BriefingQuestionRepository questionRepository;
    private BriefingAnswerRepository answerRepository;
    private AIGenerationRepository aiGenerationRepository;

    private WorkspaceId workspaceId;
    private ClientId clientId;
    private ServiceType serviceType;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(BriefingSessionRepository.class);
        questionRepository = mock(BriefingQuestionRepository.class);
        answerRepository = mock(BriefingAnswerRepository.class);
        aiGenerationRepository = mock(AIGenerationRepository.class);

        service = new BriefingService(
                sessionRepository,
                questionRepository,
                answerRepository,
                aiGenerationRepository
        );

        workspaceId = new WorkspaceId(UUID.randomUUID());
        clientId = new ClientId(UUID.randomUUID());
        serviceType = ServiceType.SOCIAL_MEDIA;
    }

    @Nested
    @DisplayName("Start Briefing")
    class StartBriefingTests {

        @Test
        void shouldStartNewBriefing() {
            // Given
            when(sessionRepository.findActiveByClientAndService(clientId, serviceType))
                    .thenReturn(Optional.empty());

            // When
            BriefingInProgress session = service.startBriefing(workspaceId, clientId, serviceType);

            // Then
            assertThat(session).isNotNull();
            assertThat(session.status()).isEqualTo("IN_PROGRESS");
            verify(sessionRepository).save(session);
        }

        @Test
        void shouldRejectDuplicateActiveBriefing() {
            // Given
            BriefingInProgress existingSession = BriefingSession.startNew(workspaceId, clientId, serviceType);
            when(sessionRepository.findActiveByClientAndService(clientId, serviceType))
                    .thenReturn(Optional.of(existingSession));

            // Then
            assertThatThrownBy(() -> service.startBriefing(workspaceId, clientId, serviceType))
                    .isInstanceOf(BriefingAlreadyInProgressException.class)
                    .hasMessageContaining("Only 1 active briefing");
        }

        @Test
        void shouldThrowOnNullWorkspaceId() {
            assertThatThrownBy(() -> service.startBriefing(null, clientId, serviceType))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void shouldThrowOnNullClientId() {
            assertThatThrownBy(() -> service.startBriefing(workspaceId, null, serviceType))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Get Next Question")
    class GetNextQuestionTests {

        @Test
        void shouldGetNextQuestion() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);
            BriefingQuestion question = new BriefingQuestion(
                    QuestionId.generate(), sessionId, "What is your goal?", 1, "OPEN_ENDED", Instant.now()
            );

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
            when(sessionRepository.countAnswers(sessionId)).thenReturn(0L);
            when(questionRepository.findBySessionAndStep(sessionId, 1)).thenReturn(Optional.of(question));

            // When
            BriefingQuestion result = service.getNextQuestion(sessionId);

            // Then
            assertThat(result).isEqualTo(question);
        }

        @Test
        void shouldThrowWhenSessionNotFound() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() -> service.getNextQuestion(sessionId))
                    .isInstanceOf(BriefingNotFoundException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        void shouldThrowWhenSessionNotInProgress() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingCompleted completed = mock(BriefingCompleted.class);
            when(completed.status()).thenReturn("COMPLETED");
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(completed));

            // Then
            assertThatThrownBy(() -> service.getNextQuestion(sessionId))
                    .isInstanceOf(InvalidStateException.class)
                    .hasMessageContaining("not in progress");
        }
    }

    @Nested
    @DisplayName("Submit Direct Answer")
    class SubmitDirectAnswerTests {

        @Test
        void shouldSubmitDirectAnswer() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            QuestionId questionId = QuestionId.generate();
            AnswerText text = new AnswerText("My answer");

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));

            // When
            service.submitDirectAnswer(sessionId, questionId, text, 75);

            // Then
            verify(answerRepository).save(any(AnsweredDirect.class));
        }

        @Test
        void shouldThrowWhenSessionNotFound() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() ->
                    service.submitDirectAnswer(sessionId, QuestionId.generate(), new AnswerText("test"), 75)
            ).isInstanceOf(BriefingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Submit Answer With Followup")
    class SubmitAnswerWithFollowupTests {

        @Test
        void shouldSubmitAnswerWithFollowup() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            QuestionId questionId = QuestionId.generate();
            AnswerText text = new AnswerText("My answer");
            BriefingQuestion followup = new BriefingQuestion(
                    QuestionId.generate(), sessionId, "Follow-up?", 2, "OPEN_ENDED", Instant.now()
            );

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));
            when(answerRepository.countFollowupsByQuestion(questionId)).thenReturn(0L);

            // When
            service.submitAnswerWithFollowup(sessionId, questionId, text, followup, 85);

            // Then
            verify(answerRepository).save(any(AnsweredWithFollowup.class));
            verify(questionRepository).save(followup);
        }

        @Test
        void shouldRejectExcessFollowups() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            QuestionId questionId = QuestionId.generate();
            BriefingQuestion followup = new BriefingQuestion(
                    QuestionId.generate(), sessionId, "Q?", 2, "OPEN_ENDED", Instant.now()
            );

            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));
            when(answerRepository.countFollowupsByQuestion(questionId)).thenReturn(1L);

            // Then
            assertThatThrownBy(() ->
                    service.submitAnswerWithFollowup(sessionId, questionId, new AnswerText("test"), followup, 85)
            ).isInstanceOf(MaxFollowupExceededException.class)
                    .hasMessageContaining("Max 1");
        }
    }

    @Nested
    @DisplayName("Detect Gaps")
    class DetectGapsTests {

        @Test
        void shouldReturnGapAnalysis_whenNoAnswers() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));
            when(answerRepository.findBySession(sessionId))
                    .thenReturn(List.of()); // 0 answers

            // When
            GapAnalysis analysis = service.detectGaps(sessionId);

            // Then — detectGaps ALWAYS returns non-null GapAnalysis (C3 fix)
            assertThat(analysis).isNotNull();
            assertThat(analysis.score()).isEqualTo(0); // 0 of 10 answers → 0%
            assertThat(analysis.gaps()).isNotEmpty();
            assertThat(analysis.isEligibleForCompletion()).isFalse();
        }

        @Test
        void shouldReturnEligible_whenEnoughAnswers() {
            // Given — 10 answers → score = 100%, eligible for completion
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));

            List<BriefingAnswer> tenAnswers = java.util.stream.IntStream.range(0, 10)
                    .mapToObj(i -> (BriefingAnswer) new AnsweredDirect(
                            AnswerId.generate(), sessionId, QuestionId.generate(),
                            new AnswerText("answer " + i), java.time.Instant.now(), 80
                    ))
                    .toList();
            when(answerRepository.findBySession(sessionId)).thenReturn(tenAnswers);

            // When
            GapAnalysis analysis = service.detectGaps(sessionId);

            // Then
            assertThat(analysis).isNotNull();
            assertThat(analysis.score()).isEqualTo(100);
            assertThat(analysis.gaps()).isEmpty();
            assertThat(analysis.isEligibleForCompletion()).isTrue();
        }

        @Test
        void shouldReturnPartialScore_withSomeAnswers() {
            // Given — 5 answers → score = 50%, not eligible
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId))
                    .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, serviceType)));

            List<BriefingAnswer> fiveAnswers = java.util.stream.IntStream.range(0, 5)
                    .mapToObj(i -> (BriefingAnswer) new AnsweredDirect(
                            AnswerId.generate(), sessionId, QuestionId.generate(),
                            new AnswerText("answer " + i), java.time.Instant.now(), 80
                    ))
                    .toList();
            when(answerRepository.findBySession(sessionId)).thenReturn(fiveAnswers);

            // When
            GapAnalysis analysis = service.detectGaps(sessionId);

            // Then
            assertThat(analysis).isNotNull();
            assertThat(analysis.score()).isEqualTo(50);
            assertThat(analysis.isEligibleForCompletion()).isFalse();
        }

        @Test
        void shouldThrowWhenSessionNotFound() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

            // Then
            assertThatThrownBy(() -> service.detectGaps(sessionId))
                    .isInstanceOf(BriefingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Complete Briefing")
    class CompleteBriefingTests {

        @Test
        void shouldCompleteBriefing() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);
            CompletionScore score = new CompletionScore(85, List.of());

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            // When
            BriefingCompleted completed = service.completeBriefing(sessionId, score);

            // Then
            assertThat(completed.status()).isEqualTo("COMPLETED");
            assertThat(completed.getCompletionScore()).isEqualTo(score);
            verify(sessionRepository).save(completed);
        }

        @Test
        void shouldRejectCompletionWithLowScore() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            CompletionScore score = mock(CompletionScore.class);
            when(score.score()).thenReturn(79);

            // Then
            assertThatThrownBy(() -> service.completeBriefing(sessionId, score))
                    .isInstanceOf(IncompleteGapsException.class)
                    .hasMessageContaining("< 80%");
        }

        @Test
        void shouldRejectCompletionWhenAlreadyCompleted() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingCompleted completed = mock(BriefingCompleted.class);
            when(completed.status()).thenReturn("COMPLETED");
            CompletionScore score = new CompletionScore(85, List.of());

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(completed));

            // Then
            assertThatThrownBy(() -> service.completeBriefing(sessionId, score))
                    .isInstanceOf(BriefingAlreadyCompletedException.class)
                    .hasMessageContaining("already completed");
        }
    }

    @Nested
    @DisplayName("Abandon Briefing")
    class AbandonBriefingTests {

        @Test
        void shouldAbandonBriefing() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

            // When
            BriefingAbandoned abandoned = service.abandonBriefing(sessionId);

            // Then
            assertThat(abandoned.status()).isEqualTo("ABANDONED");
            verify(sessionRepository).save(abandoned);
        }

        @Test
        void shouldThrowWhenAbandoningCompleted() {
            // Given
            BriefingSessionId sessionId = BriefingSessionId.generate();
            BriefingCompleted completed = mock(BriefingCompleted.class);
            when(completed.status()).thenReturn("COMPLETED");

            when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(completed));

            // Then
            assertThatThrownBy(() -> service.abandonBriefing(sessionId))
                    .isInstanceOf(BriefingAlreadyCompletedException.class)
                    .hasMessageContaining("Cannot abandon completed");
        }
    }

    @Nested
    @DisplayName("Record AI Generation")
    class RecordAIGenerationTests {

        @Test
        void shouldRecordAIGeneration() {
            // Given
            AIGeneration generation = new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{}",
                    "{}",
                    "v1",
                    500,
                    new BigDecimal("0.001")
            );

            // When
            service.recordAIGeneration(generation);

            // Then
            verify(aiGenerationRepository).save(generation);
        }

        @Test
        void shouldThrowOnNullGeneration() {
            // Then
            assertThatThrownBy(() -> service.recordAIGeneration(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
