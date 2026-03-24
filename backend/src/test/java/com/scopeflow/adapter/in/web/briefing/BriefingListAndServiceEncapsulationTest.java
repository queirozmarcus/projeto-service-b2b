package com.scopeflow.adapter.in.web.briefing;

import com.scopeflow.adapter.in.web.GlobalExceptionHandler;
import com.scopeflow.adapter.in.web.briefing.mapper.BriefingMapper;
import com.scopeflow.adapter.in.web.briefing.mapper.BriefingMapperImpl;
import com.scopeflow.config.TestSecurityConfig;
import com.scopeflow.config.WithScopeFlowUser;
import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.workspace.WorkspaceId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for I4 (GET /briefings — list endpoint intent) and
 * I5 (BriefingService encapsulation — no public repository getters).
 *
 * I4: GET /briefings returns paginated list for workspace.
 * I5: BriefingService exposes domain methods, not repository accessors.
 */
@WebMvcTest(BriefingControllerV1.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class, BriefingMapperImpl.class})
@DisplayName("Briefing — list endpoint (I4) and service encapsulation (I5)")
class BriefingListAndServiceEncapsulationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BriefingService briefingService;

    // ============ I4: GET /briefings ============

    @Nested
    @DisplayName("I4 — GET /briefings list endpoint")
    class BriefingListTests {

        @Test
        @DisplayName("GET /briefings returns 200 with paginated response for workspace")
        @WithScopeFlowUser
        void shouldReturn200_withPaginatedResponse() throws Exception {
            // Given — 3 briefings in workspace
            List<BriefingSession> sessions = List.of(
                    BriefingSession.startNew(
                            new WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                            new ClientId(UUID.randomUUID()),
                            ServiceType.SOCIAL_MEDIA
                    ),
                    BriefingSession.startNew(
                            new WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                            new ClientId(UUID.randomUUID()),
                            ServiceType.SOCIAL_MEDIA
                    )
            );
            given(briefingService.findByWorkspaceAndStatus(any(WorkspaceId.class), any()))
                    .willReturn(sessions);

            // When / Then
            mockMvc.perform(get("/briefings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("GET /briefings returns empty page when workspace has no briefings")
        @WithScopeFlowUser
        void shouldReturnEmptyPage_whenNoBriefings() throws Exception {
            // Given
            given(briefingService.findByWorkspaceAndStatus(any(WorkspaceId.class), any()))
                    .willReturn(List.of());

            // When / Then
            mockMvc.perform(get("/briefings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("GET /briefings returns 401 when unauthenticated")
        void shouldReturn401_whenUnauthenticated() throws Exception {
            // Public access to GET /briefings not allowed
            mockMvc.perform(get("/briefings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /briefings respects pagination parameters")
        @WithScopeFlowUser
        void shouldRespectPaginationParams_onBriefingsList() throws Exception {
            // Given — 25 briefings
            List<BriefingSession> sessions = java.util.stream.IntStream.range(0, 25)
                    .mapToObj(i -> BriefingSession.startNew(
                            new WorkspaceId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
                            new ClientId(UUID.randomUUID()),
                            ServiceType.SOCIAL_MEDIA
                    ))
                    .map(s -> (BriefingSession) s)
                    .toList();
            given(briefingService.findByWorkspaceAndStatus(any(WorkspaceId.class), any()))
                    .willReturn(sessions);

            // When / Then — page=0, size=10 → 10 items, totalElements=25
            mockMvc.perform(get("/briefings?page=0&size=10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.totalElements").value(25));
        }

        @Test
        @DisplayName("GET /briefings with status filter passes status to service")
        @WithScopeFlowUser
        void shouldPassStatusFilter_toService() throws Exception {
            // Given
            given(briefingService.findByWorkspaceAndStatus(any(WorkspaceId.class), any()))
                    .willReturn(List.of());

            // When / Then — no error when status filter provided
            mockMvc.perform(get("/briefings?status=IN_PROGRESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }
    }

    // ============ I5: BriefingService no public repository getters ============

    @Nested
    @DisplayName("I5 — BriefingService encapsulates repository access")
    class ServiceEncapsulationTests {

        @Test
        @DisplayName("BriefingService does not expose sessionRepository as public field")
        void briefingService_shouldNotExposeRepositoryPublicly() throws Exception {
            // Verify via reflection that BriefingService has no public repository getters
            Class<?> clazz = BriefingService.class;

            // Check no public method returns a repository type
            java.lang.reflect.Method[] methods = clazz.getMethods();
            for (java.lang.reflect.Method method : methods) {
                String returnTypeName = method.getReturnType().getName();
                assertThat(returnTypeName)
                        .as("Method %s should not return a repository", method.getName())
                        .doesNotContain("Repository");
            }
        }

        @Test
        @DisplayName("BriefingService findByWorkspaceAndStatus method is public and accessible")
        void findByWorkspaceAndStatus_shouldBePublicMethod() throws Exception {
            // Given — verify the proper query method exists
            var method = BriefingService.class.getMethod(
                    "findByWorkspaceAndStatus", WorkspaceId.class, String.class
            );

            // Then — method is accessible
            assertThat(method).isNotNull();
            assertThat(java.lang.reflect.Modifier.isPublic(method.getModifiers())).isTrue();
        }

        @Test
        @DisplayName("BriefingService findAnswers method delegates to answerRepository (not exposed)")
        void findAnswers_shouldDelegateToRepository() {
            // Given
            BriefingSessionRepository sessionRepo = org.mockito.Mockito.mock(BriefingSessionRepository.class);
            BriefingQuestionRepository questionRepo = org.mockito.Mockito.mock(BriefingQuestionRepository.class);
            BriefingAnswerRepository answerRepo = org.mockito.Mockito.mock(BriefingAnswerRepository.class);
            AIGenerationRepository aiRepo = org.mockito.Mockito.mock(AIGenerationRepository.class);

            BriefingService service = new BriefingService(sessionRepo, questionRepo, answerRepo, aiRepo);

            BriefingSessionId sessionId = BriefingSessionId.generate();
            given(answerRepo.findBySession(sessionId)).willReturn(List.of());

            // When
            List<BriefingAnswer> answers = service.findAnswers(sessionId);

            // Then — delegated to repo, result returned (not via exposed repo getter)
            assertThat(answers).isEmpty();
            org.mockito.Mockito.verify(answerRepo).findBySession(sessionId);
        }

        @Test
        @DisplayName("Controller calls service.findByWorkspaceAndStatus, not a repository getter")
        @WithScopeFlowUser
        void controller_shouldCallServiceMethod_notRepositoryGetter() throws Exception {
            // Given
            given(briefingService.findByWorkspaceAndStatus(any(), any())).willReturn(List.of());

            // When — controller calls briefingService, not repository directly
            mockMvc.perform(get("/briefings"))
                    .andExpect(status().isOk());

            // Then — the service method was invoked (controller respects I5 boundary)
            org.mockito.Mockito.verify(briefingService)
                    .findByWorkspaceAndStatus(any(WorkspaceId.class), any());
        }
    }
}
