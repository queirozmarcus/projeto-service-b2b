package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Extended unit tests for C3: detectGaps ALWAYS returns non-null GapAnalysis.
 *
 * Covers: 0 answers (0%), partial (50%), full (100%), over-answer capped,
 * session not found throws, null check on result.
 */
@DisplayName("BriefingService.detectGaps (C3 — never null)")
class DetectGapsTest {

    private BriefingService service;
    private BriefingSessionRepository sessionRepository;
    private BriefingAnswerRepository answerRepository;

    private WorkspaceId workspaceId;
    private ClientId clientId;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(BriefingSessionRepository.class);
        BriefingQuestionRepository questionRepository = mock(BriefingQuestionRepository.class);
        answerRepository = mock(BriefingAnswerRepository.class);
        AIGenerationRepository aiRepo = mock(AIGenerationRepository.class);

        service = new BriefingService(sessionRepository, questionRepository, answerRepository, aiRepo);
        workspaceId = new WorkspaceId(UUID.randomUUID());
        clientId = new ClientId(UUID.randomUUID());
    }

    @Test
    @DisplayName("0 answers → score 0%, gaps non-empty, not eligible for completion")
    void withZeroAnswers_shouldReturnZeroScoreWithGaps() {
        // Given
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));
        when(answerRepository.findBySession(sessionId)).thenReturn(List.of());

        // When
        GapAnalysis analysis = service.detectGaps(sessionId);

        // Then
        assertThat(analysis).isNotNull(); // C3: NEVER null
        assertThat(analysis.score()).isEqualTo(0);
        assertThat(analysis.gaps()).isNotEmpty();
        assertThat(analysis.isEligibleForCompletion()).isFalse();
    }

    @Test
    @DisplayName("5 of 10 answers → score 50%, not eligible for completion")
    void withFiveAnswers_shouldReturnFiftyPercent() {
        // Given
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));

        List<BriefingAnswer> fiveAnswers = buildAnswers(sessionId, 5);
        when(answerRepository.findBySession(sessionId)).thenReturn(fiveAnswers);

        // When
        GapAnalysis analysis = service.detectGaps(sessionId);

        // Then
        assertThat(analysis).isNotNull();
        assertThat(analysis.score()).isEqualTo(50);
        assertThat(analysis.isEligibleForCompletion()).isFalse();
    }

    @Test
    @DisplayName("8 of 10 answers → score 80%, eligible for completion (threshold exactly met)")
    void withEightAnswers_shouldReturnEightyPercent_eligibleForCompletion() {
        // Given
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));

        List<BriefingAnswer> eightAnswers = buildAnswers(sessionId, 8);
        when(answerRepository.findBySession(sessionId)).thenReturn(eightAnswers);

        // When
        GapAnalysis analysis = service.detectGaps(sessionId);

        // Then
        assertThat(analysis).isNotNull();
        assertThat(analysis.score()).isEqualTo(80);
        assertThat(analysis.isEligibleForCompletion()).isTrue();
    }

    @Test
    @DisplayName("10 of 10 answers → score 100%, gaps empty, eligible for completion")
    void withAllAnswers_shouldReturnHundredPercent_noGaps() {
        // Given
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));

        List<BriefingAnswer> tenAnswers = buildAnswers(sessionId, 10);
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
    @DisplayName("more than 10 answers → score capped at 100% (Math.min guard)")
    void withMoreThanTenAnswers_shouldCapScoreAt100() {
        // Given — 15 answers (over expected 10)
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));

        List<BriefingAnswer> fifteenAnswers = buildAnswers(sessionId, 15);
        when(answerRepository.findBySession(sessionId)).thenReturn(fifteenAnswers);

        // When
        GapAnalysis analysis = service.detectGaps(sessionId);

        // Then — score must be capped at 100, not 150
        assertThat(analysis).isNotNull();
        assertThat(analysis.score()).isEqualTo(100);
        assertThat(analysis.score()).isLessThanOrEqualTo(100);
    }

    @Test
    @DisplayName("detectGaps throws BriefingNotFoundException when session does not exist")
    void withUnknownSession_shouldThrowBriefingNotFoundException() {
        // Given
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> service.detectGaps(sessionId))
                .isInstanceOf(BriefingNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("detectGaps throws NullPointerException when sessionId is null")
    void withNullSessionId_shouldThrowNPE() {
        assertThatThrownBy(() -> service.detectGaps(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("result GapAnalysis gaps message contains count of remaining answers needed")
    void gapMessageShouldDescribeRemainingAnswers() {
        // Given — 3 of 10 answers → 7 remaining
        BriefingSessionId sessionId = BriefingSessionId.generate();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(BriefingSession.startNew(workspaceId, clientId, ServiceType.SOCIAL_MEDIA)));

        List<BriefingAnswer> threeAnswers = buildAnswers(sessionId, 3);
        when(answerRepository.findBySession(sessionId)).thenReturn(threeAnswers);

        // When
        GapAnalysis analysis = service.detectGaps(sessionId);

        // Then
        assertThat(analysis.gaps())
                .anyMatch(gap -> gap.contains("7") || gap.contains("more answer"));
    }

    // ============ Helpers ============

    private List<BriefingAnswer> buildAnswers(BriefingSessionId sessionId, int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> (BriefingAnswer) new AnsweredDirect(
                        AnswerId.generate(),
                        sessionId,
                        QuestionId.generate(),
                        new AnswerText("answer " + i),
                        Instant.now(),
                        75
                ))
                .toList();
    }
}
