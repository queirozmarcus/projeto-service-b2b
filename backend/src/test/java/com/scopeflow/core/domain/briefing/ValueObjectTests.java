package com.scopeflow.core.domain.briefing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Briefing value objects (records).
 */
@DisplayName("Briefing Value Objects Tests")
class ValueObjectTests {

    @Nested
    @DisplayName("BriefingSessionId")
    class BriefingSessionIdTests {

        @Test
        void shouldCreateFromUUID() {
            // When
            UUID uuid = UUID.randomUUID();
            BriefingSessionId id = new BriefingSessionId(uuid);

            // Then
            assertThat(id.value()).isEqualTo(uuid);
        }

        @Test
        void shouldGenerateUniqueIds() {
            // When
            BriefingSessionId id1 = BriefingSessionId.generate();
            BriefingSessionId id2 = BriefingSessionId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
            assertThat(id1.value()).isNotEqualTo(id2.value());
        }

        @Test
        void shouldParseFromString() {
            // Given
            String uuidString = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

            // When
            BriefingSessionId id = BriefingSessionId.of(uuidString);

            // Then
            assertThat(id.value().toString()).isEqualTo(uuidString);
        }

        @Test
        void shouldRejectNullValue() {
            assertThatThrownBy(() -> new BriefingSessionId(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("AnswerText")
    class AnswerTextTests {

        @Test
        void shouldCreateValidAnswerText() {
            // When
            AnswerText text = new AnswerText("This is my answer");

            // Then
            assertThat(text.value()).isEqualTo("This is my answer");
        }

        @Test
        void shouldRejectBlankAnswer() {
            assertThatThrownBy(() -> new AnswerText(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        void shouldRejectNullAnswer() {
            assertThatThrownBy(() -> new AnswerText(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldRejectWhitespaceOnlyAnswer() {
            assertThatThrownBy(() -> new AnswerText("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be empty");
        }

        @Test
        void shouldRejectAnswerExceeding5000Chars() {
            // Given
            String tooLong = "x".repeat(5001);

            // Then
            assertThatThrownBy(() -> new AnswerText(tooLong))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("5000");
        }

        @Test
        void shouldAcceptAnswer5000CharsExactly() {
            // Given
            String exact = "x".repeat(5000);

            // When & Then
            assertThatCode(() -> new AnswerText(exact))
                    .doesNotThrowAnyException();
        }

        @Test
        void shouldTrimWhitespace() {
            // When
            AnswerText text = new AnswerText("  My answer  ");

            // Then
            assertThat(text.trimmed()).isEqualTo("My answer");
        }
    }

    @Nested
    @DisplayName("PublicToken")
    class PublicTokenTests {

        @Test
        void shouldGenerateSecureToken() {
            // When
            PublicToken token = PublicToken.generate();

            // Then
            assertThat(token.value()).isNotNull();
            assertThat(token.value().length()).isGreaterThanOrEqualTo(32);
        }

        @Test
        void shouldGenerateUniqueTokens() {
            // When
            PublicToken token1 = PublicToken.generate();
            PublicToken token2 = PublicToken.generate();

            // Then
            assertThat(token1).isNotEqualTo(token2);
            assertThat(token1.value()).isNotEqualTo(token2.value());
        }

        @Test
        void shouldRejectTokenBelowMinimumLength() {
            assertThatThrownBy(() -> new PublicToken("short"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 characters");
        }

        @Test
        void shouldRejectNullToken() {
            assertThatThrownBy(() -> new PublicToken(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("BriefingProgress")
    class BriefingProgressTests {

        @Test
        void shouldCreateValidProgress() {
            // When
            BriefingProgress progress = BriefingProgress.of(5, 10);

            // Then
            assertThat(progress.currentStep()).isEqualTo(5);
            assertThat(progress.totalSteps()).isEqualTo(10);
            assertThat(progress.completionPercentage()).isEqualTo(50);
        }

        @Test
        void shouldCalculatePercentageForZeroSteps() {
            // When
            BriefingProgress progress = BriefingProgress.of(0, 0);

            // Then
            assertThat(progress.completionPercentage()).isEqualTo(0);
        }

        @Test
        void shouldCalculatePercentageFor100Percent() {
            // When
            BriefingProgress progress = BriefingProgress.of(10, 10);

            // Then
            assertThat(progress.completionPercentage()).isEqualTo(100);
        }

        @Test
        void shouldRejectInvalidStep() {
            assertThatThrownBy(() -> BriefingProgress.of(11, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("totalSteps");
        }

        @Test
        void shouldRejectNegativeStep() {
            assertThatThrownBy(() -> BriefingProgress.of(-1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldAdvanceToNextStep() {
            // Given
            BriefingProgress progress = BriefingProgress.of(3, 10);

            // When
            BriefingProgress next = progress.nextStep();

            // Then
            assertThat(next.currentStep()).isEqualTo(4);
            assertThat(next.totalSteps()).isEqualTo(10);
            assertThat(next.completionPercentage()).isEqualTo(40);
        }

        @Test
        void shouldRejectAdvanceWhenAtEnd() {
            // Given
            BriefingProgress progress = BriefingProgress.of(10, 10);

            // Then
            assertThatThrownBy(progress::nextStep)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot advance");
        }
    }

    @Nested
    @DisplayName("CompletionScore")
    class CompletionScoreTests {

        @Test
        void shouldCreateValidScore() {
            // When
            CompletionScore score = new CompletionScore(85, java.util.List.of());

            // Then
            assertThat(score.score()).isEqualTo(85);
            assertThat(score.gapsIdentified()).isEmpty();
            assertThat(score.isPerfect()).isFalse();
        }

        @Test
        void shouldIdentifyPerfectScore() {
            // When
            CompletionScore score = new CompletionScore(95, java.util.List.of());

            // Then
            assertThat(score.isPerfect()).isTrue();
        }

        @Test
        void shouldTrackGaps() {
            // When
            CompletionScore score = new CompletionScore(85, java.util.List.of("Gap 1", "Gap 2"));

            // Then
            assertThat(score.hasGaps()).isTrue();
            assertThat(score.gapsIdentified()).hasSize(2);
        }

        @Test
        void shouldRejectScoreBelowMinimum() {
            assertThatThrownBy(() -> new CompletionScore(79, java.util.List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("80");
        }

        @Test
        void shouldRejectScoreAboveMaximum() {
            assertThatThrownBy(() -> new CompletionScore(101, java.util.List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void shouldMakeGapsImmutable() {
            // Given
            var mutableGaps = new java.util.ArrayList<>(java.util.List.of("Gap 1"));
            CompletionScore score = new CompletionScore(85, mutableGaps);

            // When - modify the original list
            mutableGaps.add("Gap 2");

            // Then - score should still have only 1 gap
            assertThat(score.gapsIdentified()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("AIGeneration")
    class AIGenerationTests {

        @Test
        void shouldCreateValidAIGeneration() {
            // When
            AIGeneration gen = new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{\"question_id\": \"123\"}",
                    "{\"gaps\": [\"timeline\"]}",
                    "v1",
                    500,
                    new BigDecimal("0.001")
            );

            // Then
            assertThat(gen.type()).isEqualTo(GenerationType.GAP_ANALYSIS);
            assertThat(gen.wasFast()).isTrue();
            assertThat(gen.wasSlow()).isFalse();
        }

        @Test
        void shouldIdentifyFastGeneration() {
            // When
            AIGeneration gen = new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{}",
                    "{}",
                    "v1",
                    500,
                    BigDecimal.ZERO
            );

            // Then
            assertThat(gen.wasFast()).isTrue();
        }

        @Test
        void shouldIdentifySlowGeneration() {
            // When
            AIGeneration gen = new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{}",
                    "{}",
                    "v1",
                    6000,
                    BigDecimal.ZERO
            );

            // Then
            assertThat(gen.wasSlow()).isTrue();
        }

        @Test
        void shouldRejectNegativeLatency() {
            assertThatThrownBy(() -> new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{}",
                    "{}",
                    "v1",
                    -1,
                    BigDecimal.ZERO
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(">= 0");
        }

        @Test
        void shouldRejectNegativeCost() {
            assertThatThrownBy(() -> new AIGeneration(
                    GenerationType.GAP_ANALYSIS,
                    "{}",
                    "{}",
                    "v1",
                    500,
                    new BigDecimal("-0.001")
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(">= 0");
        }
    }

    @Nested
    @DisplayName("ID Value Objects")
    class IdValueObjectsTests {

        @Test
        void shouldCreateUniqueQuestionIds() {
            // When
            QuestionId id1 = QuestionId.generate();
            QuestionId id2 = QuestionId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void shouldCreateUniqueAnswerIds() {
            // When
            AnswerId id1 = AnswerId.generate();
            AnswerId id2 = AnswerId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }

        @Test
        void shouldCreateUniqueClientIds() {
            // When
            ClientId id1 = ClientId.generate();
            ClientId id2 = ClientId.generate();

            // Then
            assertThat(id1).isNotEqualTo(id2);
        }
    }
}
