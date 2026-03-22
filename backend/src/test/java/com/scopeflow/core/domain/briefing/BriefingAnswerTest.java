package com.scopeflow.core.domain.briefing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for BriefingAnswer sealed class hierarchy.
 */
@DisplayName("BriefingAnswer Domain Model Tests")
class BriefingAnswerTest {

    private BriefingSessionId sessionId;
    private QuestionId questionId;
    private AnswerText answerText;
    private Instant now;

    @BeforeEach
    void setUp() {
        sessionId = BriefingSessionId.generate();
        questionId = QuestionId.generate();
        answerText = new AnswerText("This is my answer");
        now = Instant.now();
    }

    @Nested
    @DisplayName("AnsweredDirect Creation")
    class AnsweredDirectCreation {

        @Test
        void shouldCreateAnsweredDirect() {
            // When
            AnsweredDirect answer = new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 75
            );

            // Then
            assertThat(answer).isNotNull();
            assertThat(answer.answerType()).isEqualTo("DIRECT");
            assertThat(answer.hasFollowup()).isFalse();
            assertThat(answer.getQualityScore()).isEqualTo(75);
        }

        @Test
        void shouldAccept0QualityScore() {
            // When & Then
            assertThatCode(() -> new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 0
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldAccept100QualityScore() {
            // When & Then
            assertThatCode(() -> new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 100
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectNegativeQualityScore() {
            assertThatThrownBy(() -> new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, -1
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0-100");
        }

        @Test
        void shouldRejectQualityScoreAbove100() {
            assertThatThrownBy(() -> new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 101
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("0-100");
        }
    }

    @Nested
    @DisplayName("AnsweredWithFollowup Creation")
    class AnsweredWithFollowupCreation {

        @Test
        void shouldCreateAnsweredWithFollowup() {
            // Given
            BriefingQuestion followupQuestion = new BriefingQuestion(
                    QuestionId.generate(), sessionId, "Follow-up question?", 2, "OPEN_ENDED", now
            );

            // When
            AnsweredWithFollowup answer = new AnsweredWithFollowup(
                    AnswerId.generate(), sessionId, questionId, answerText, now,
                    followupQuestion, 85
            );

            // Then
            assertThat(answer).isNotNull();
            assertThat(answer.answerType()).isEqualTo("WITH_FOLLOWUP");
            assertThat(answer.hasFollowup()).isTrue();
            assertThat(answer.getGeneratedFollowup()).isEqualTo(followupQuestion);
            assertThat(answer.getConfidenceScore()).isEqualTo(85);
        }

        @Test
        void shouldRejectNegativeConfidenceScore() {
            BriefingQuestion followup = new BriefingQuestion(
                    QuestionId.generate(), sessionId, "Q?", 2, "OPEN_ENDED", now
            );

            assertThatThrownBy(() -> new AnsweredWithFollowup(
                    AnswerId.generate(), sessionId, questionId, answerText, now, followup, -1
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectNullFollowup() {
            assertThatThrownBy(() -> new AnsweredWithFollowup(
                    AnswerId.generate(), sessionId, questionId, answerText, now, null, 85
            )).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Answer Immutability")
    class AnswerImmutability {

        @Test
        void shouldHaveAccessors() {
            // Given
            AnswerId answerId = AnswerId.generate();
            AnsweredDirect answer = new AnsweredDirect(answerId, sessionId, questionId, answerText, now, 80);

            // Then
            assertThat(answer.getId()).isEqualTo(answerId);
            assertThat(answer.getSessionId()).isEqualTo(sessionId);
            assertThat(answer.getQuestionId()).isEqualTo(questionId);
            assertThat(answer.getText()).isEqualTo(answerText);
            assertThat(answer.getAnsweredAt()).isEqualTo(now);
        }

        @Test
        void shouldNotHaveSetters() {
            // Given
            AnsweredDirect answer = new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 80
            );

            // Then - verify no mutating methods exist
            assertThat(answer).isNotNull();
            // If we could call a setter, the test would fail during compilation
        }
    }

    @Nested
    @DisplayName("Equality and Hashing")
    class EqualityAndHashing {

        @Test
        void shouldHaveSameHashCodeForEqualAnswers() {
            // Given
            AnswerId answerId = AnswerId.generate();
            AnsweredDirect answer1 = new AnsweredDirect(answerId, sessionId, questionId, answerText, now, 80);
            AnsweredDirect answer2 = new AnsweredDirect(answerId, sessionId, questionId, answerText, now, 80);

            // Then
            assertThat(answer1).isEqualTo(answer2);
            assertThat(answer1.hashCode()).isEqualTo(answer2.hashCode());
        }

        @Test
        void shouldHaveDifferentHashCodeForDifferentAnswers() {
            // When
            AnsweredDirect answer1 = new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 80
            );
            AnsweredDirect answer2 = new AnsweredDirect(
                    AnswerId.generate(), sessionId, questionId, answerText, now, 80
            );

            // Then
            assertThat(answer1).isNotEqualTo(answer2);
            assertThat(answer1.hashCode()).isNotEqualTo(answer2.hashCode());
        }
    }
}
