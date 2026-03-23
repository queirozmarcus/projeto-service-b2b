package com.scopeflow.adapter.in.web.briefing.mapper;

import com.scopeflow.adapter.in.web.briefing.dto.*;
import com.scopeflow.adapter.in.web.briefing.fixtures.BriefingTestFixtures;
import com.scopeflow.core.domain.briefing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BriefingMapperImpl.
 *
 * Tests domain ↔ DTO conversions without Spring context.
 */
@DisplayName("BriefingMapper — Domain ↔ DTO conversions")
class BriefingMapperTest {

    private BriefingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BriefingMapperImpl();
    }

    @Test
    @DisplayName("toResponse — should map BriefingInProgress to BriefingResponse with all fields")
    void testMapBriefingSessionToDTO() {
        // Given
        var session = BriefingTestFixtures.createTestBriefingInProgress();

        // When
        var response = mapper.toResponse(session);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(session.getId().value());
        assertThat(response.workspaceId()).isEqualTo(session.getWorkspaceId().value());
        assertThat(response.clientId()).isEqualTo(session.getClientId().value());
        assertThat(response.serviceType()).isEqualTo(session.getServiceType().name());
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.publicToken()).isEqualTo(session.getPublicToken().value());
        assertThat(response.completionScore()).isNull(); // Not completed yet
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("toResponse — should map BriefingCompleted with completion score")
    void testMapBriefingCompletedToDTO() {
        // Given
        var completed = BriefingTestFixtures.createTestBriefingCompleted();

        // When
        var response = mapper.toResponse(completed);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completionScore()).isEqualTo(95);
    }

    @Test
    @DisplayName("toDetailResponse — should include nested progress, questions, answers")
    void testMapBriefingDetailResponseDTO() {
        // Given
        var session = BriefingTestFixtures.createTestBriefingInProgress();
        var question1 = BriefingTestFixtures.createTestQuestion(session.getId(), 1);
        var question2 = BriefingTestFixtures.createTestQuestion(session.getId(), 2);
        var answer1 = BriefingTestFixtures.createTestAnswer(session.getId(), question1.getId());

        // When
        var response = mapper.toDetailResponse(session, List.of(question1, question2), List.of(answer1));

        // Then
        assertThat(response).isNotNull();
        assertThat(response.briefing()).isNotNull();
        assertThat(response.progress()).isNotNull();
        assertThat(response.questions()).hasSize(2);
        assertThat(response.answers()).hasSize(1);
    }

    @Test
    @DisplayName("toPublicResponse — should NOT include sensitive fields (workspaceId, clientId)")
    void testMapPublicBriefingResponse() {
        // Given
        var session = BriefingTestFixtures.createTestBriefingInProgress();

        // When
        var response = mapper.toPublicResponse(session);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(session.getId().value());
        assertThat(response.serviceType()).isEqualTo(session.getServiceType().name());
        assertThat(response.status()).isEqualTo("IN_PROGRESS");
        assertThat(response.progress()).isNotNull();
        assertThat(response.createdAt()).isNotNull();

        // Sensitive fields should NOT be present in PublicBriefingResponse
        // (verified by record structure in PublicBriefingResponse.java)
    }

    @Test
    @DisplayName("toProgressResponse — should calculate percentage correctly")
    void testMapProgressResponse() {
        // Given
        var score = new CompletionScore(85, List.of("Need more context"));

        // When
        var response = mapper.toProgressResponse(score);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.completionPercentage()).isEqualTo(85);
        assertThat(response.gapsIdentified()).hasSize(1);
        assertThat(response.gapsIdentified().get(0)).isEqualTo("Need more context");
    }

    @Test
    @DisplayName("toQuestionResponse — should map enum correctly")
    void testMapQuestionResponse() {
        // Given
        var session = BriefingTestFixtures.createTestBriefingInProgress();
        var question = BriefingTestFixtures.createTestQuestion(session.getId(), 1);

        // When
        var response = mapper.toQuestionResponse(question, false);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(question.getId().value());
        assertThat(response.text()).isEqualTo(question.getText());
        assertThat(response.step()).isEqualTo(1);
        assertThat(response.questionType()).isEqualTo("OPEN");
        assertThat(response.required()).isTrue();
        assertThat(response.followUpGenerated()).isFalse();
    }

    @Test
    @DisplayName("toAnswerResponse — should handle nullable qualityScore")
    void testMapAnswerResponse() {
        // Given
        var session = BriefingTestFixtures.createTestBriefingInProgress();
        var question = BriefingTestFixtures.createTestQuestion(session.getId(), 1);
        var answer = BriefingTestFixtures.createTestAnswer(session.getId(), question.getId());

        // When
        var response = mapper.toAnswerResponse(answer);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(answer.getId().value());
        assertThat(response.questionId()).isEqualTo(question.getId().value());
        assertThat(response.answerText()).isEqualTo("Test answer text");
        assertThat(response.qualityScore()).isEqualTo(85);
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("toPageResponse — should preserve pagination metadata")
    void testMapPageResponse() {
        // Given
        var content = List.of(
                mapper.toResponse(BriefingTestFixtures.createTestBriefingInProgress()),
                mapper.toResponse(BriefingTestFixtures.createTestBriefingInProgress())
        );

        // When
        var page = mapper.toPageResponse(content, 50, 3, 20, 0, true, false);

        // Then
        assertThat(page).isNotNull();
        assertThat(page.content()).hasSize(2);
        assertThat(page.totalElements()).isEqualTo(50);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.number()).isEqualTo(0);
        assertThat(page.first()).isTrue();
        assertThat(page.last()).isFalse();
    }

    @Test
    @DisplayName("toCompletionScore — should convert DTO to domain correctly")
    void testMapCreateBriefingRequest() {
        // Given
        var request = new CompleteBriefingRequest(85, List.of("Gap 1", "Gap 2"));

        // When
        var score = mapper.toCompletionScore(request);

        // Then
        assertThat(score).isNotNull();
        assertThat(score.score()).isEqualTo(85);
        assertThat(score.gapsIdentified()).hasSize(2);
    }

    @Test
    @DisplayName("toClientId — should convert UUID to domain ClientId")
    void testConvertClientId() {
        // Given
        var uuid = UUID.randomUUID();

        // When
        var clientId = mapper.toClientId(uuid);

        // Then
        assertThat(clientId).isNotNull();
        assertThat(clientId.value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("toServiceType — should convert string to domain enum")
    void testConvertServiceType() {
        // When
        var serviceType = mapper.toServiceType("SOCIAL_MEDIA");

        // Then
        assertThat(serviceType).isEqualTo(ServiceType.SOCIAL_MEDIA);
    }
}
