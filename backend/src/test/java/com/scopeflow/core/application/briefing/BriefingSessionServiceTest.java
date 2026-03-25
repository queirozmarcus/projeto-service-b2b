package com.scopeflow.core.application.briefing;

import com.scopeflow.adapter.out.persistence.briefing.*;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalSpringRepository;
import com.scopeflow.core.domain.briefing.BriefingNotFoundException;
import com.scopeflow.core.domain.proposal.ProposalNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BriefingSessionService.
 * All dependencies mocked — no Spring context, no DB.
 */
@ExtendWith(MockitoExtension.class)
class BriefingSessionServiceTest {

    @Mock JpaBriefingSessionSpringRepository sessionRepo;
    @Mock JpaBriefingAnswerSpringRepository answerRepo;
    @Mock JpaServiceContextProfileSpringRepository profileRepo;
    @Mock JpaServiceContextQuestionSpringRepository questionRepo;
    @Mock JpaProposalSpringRepository proposalRepo;

    BriefingSessionService service;

    UUID workspaceId = UUID.randomUUID();
    UUID proposalId = UUID.randomUUID();
    UUID sessionId = UUID.randomUUID();
    UUID clientId = UUID.randomUUID();
    UUID profileId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BriefingSessionService(
                sessionRepo, answerRepo, profileRepo, questionRepo, proposalRepo
        );
    }

    // ============ createBriefingSession ============

    @Nested
    @DisplayName("createBriefingSession")
    class CreateBriefingSession {

        @Test
        @DisplayName("should create session when proposal belongs to workspace")
        void shouldCreateSession_whenProposalBelongsToWorkspace() {
            JpaProposal proposal = mockProposal(proposalId, workspaceId, clientId, null);
            when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            JpaBriefingSession result = service.createBriefingSession(proposalId, workspaceId);

            assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
            assertThat(result.getClientId()).isEqualTo(clientId);
            assertThat(result.getStatus()).isEqualTo("IN_PROGRESS");
            assertThat(result.getPublicToken()).isNotBlank();
            verify(sessionRepo).save(any(JpaBriefingSession.class));
        }

        @Test
        @DisplayName("should throw ProposalNotFoundException when proposal does not exist")
        void shouldThrow_whenProposalNotFound() {
            when(proposalRepo.findById(proposalId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createBriefingSession(proposalId, workspaceId))
                    .isInstanceOf(ProposalNotFoundException.class);
            verify(sessionRepo, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccessDeniedException when proposal belongs to another workspace")
        void shouldThrow_whenProposalBelongsToDifferentWorkspace() {
            UUID otherWorkspace = UUID.randomUUID();
            JpaProposal proposal = mockProposal(proposalId, otherWorkspace, clientId, null);
            when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));

            assertThatThrownBy(() -> service.createBriefingSession(proposalId, workspaceId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(sessionRepo, never()).save(any());
        }

        @Test
        @DisplayName("should inherit serviceType from linked briefing session when available")
        void shouldInheritServiceType_fromLinkedBriefingSession() {
            UUID linkedBriefingId = UUID.randomUUID();
            JpaProposal proposal = mockProposal(proposalId, workspaceId, clientId, linkedBriefingId);
            JpaBriefingSession linked = mockSession(linkedBriefingId, workspaceId, clientId, "BRANDING", "COMPLETED");

            when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));
            when(sessionRepo.findById(linkedBriefingId)).thenReturn(Optional.of(linked));
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            JpaBriefingSession result = service.createBriefingSession(proposalId, workspaceId);

            assertThat(result.getServiceType()).isEqualTo("BRANDING");
        }

        @Test
        @DisplayName("should fall back to SOCIAL_MEDIA when no linked briefing session")
        void shouldFallBackToSocialMedia_whenNoLinkedBriefing() {
            JpaProposal proposal = mockProposal(proposalId, workspaceId, clientId, null);
            when(proposalRepo.findById(proposalId)).thenReturn(Optional.of(proposal));
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            JpaBriefingSession result = service.createBriefingSession(proposalId, workspaceId);

            assertThat(result.getServiceType()).isEqualTo("SOCIAL_MEDIA");
        }
    }

    // ============ getBriefingSession ============

    @Nested
    @DisplayName("getBriefingSession")
    class GetBriefingSession {

        @Test
        @DisplayName("should return session when workspace matches")
        void shouldReturnSession_whenWorkspaceMatches() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            JpaBriefingSession result = service.getBriefingSession(sessionId, workspaceId);

            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("should throw BriefingNotFoundException when session not found")
        void shouldThrow_whenNotFound() {
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBriefingSession(sessionId, workspaceId))
                    .isInstanceOf(BriefingNotFoundException.class);
        }

        @Test
        @DisplayName("should throw AccessDeniedException when session belongs to another workspace")
        void shouldThrow_whenWrongWorkspace() {
            JpaBriefingSession session = mockSession(sessionId, UUID.randomUUID(), clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.getBriefingSession(sessionId, workspaceId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ============ getQuestions ============

    @Nested
    @DisplayName("getQuestions")
    class GetQuestions {

        @Test
        @DisplayName("should return questions from ServiceContextProfile")
        void shouldReturnQuestions_whenProfileExists() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            JpaServiceContextProfile profile = mockProfile(profileId, workspaceId, "SOCIAL_MEDIA");
            JpaServiceContextQuestion q1 = mockQuestion(UUID.randomUUID(), profileId, "What is your goal?", 1, true);
            JpaServiceContextQuestion q2 = mockQuestion(UUID.randomUUID(), profileId, "Who is your audience?", 2, false);

            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.of(profile));
            when(questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profileId))
                    .thenReturn(List.of(q1, q2));

            List<JpaServiceContextQuestion> result = service.getQuestions(sessionId, workspaceId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(1);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("should return empty list when no ServiceContextProfile exists")
        void shouldReturnEmpty_whenNoProfile() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.empty());

            List<JpaServiceContextQuestion> result = service.getQuestions(sessionId, workspaceId);

            assertThat(result).isEmpty();
            verify(questionRepo, never()).findByServiceContextProfileIdOrderByOrderIndexAsc(any());
        }
    }

    // ============ submitAnswers ============

    @Nested
    @DisplayName("submitAnswers")
    class SubmitAnswers {

        @Test
        @DisplayName("should persist answers for IN_PROGRESS session")
        void shouldPersistAnswers_whenSessionInProgress() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            UUID qId = UUID.randomUUID();
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, qId)).thenReturn(false);

            List<BriefingSessionService.AnswerInput> inputs =
                    List.of(new BriefingSessionService.AnswerInput(qId, "My answer"));

            service.submitAnswers(sessionId, workspaceId, inputs);

            verify(answerRepo).save(argThat(a ->
                    a.getBriefingSessionId().equals(sessionId)
                    && a.getQuestionId().equals(qId)
                    && a.getAnswerText().equals("My answer")
            ));
        }

        @Test
        @DisplayName("should skip already-answered questions (idempotency)")
        void shouldSkipDuplicateAnswers() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            UUID qId = UUID.randomUUID();
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, qId)).thenReturn(true);

            service.submitAnswers(sessionId, workspaceId,
                    List.of(new BriefingSessionService.AnswerInput(qId, "Duplicate")));

            verify(answerRepo, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalStateException when session is not IN_PROGRESS")
        void shouldThrow_whenSessionNotInProgress() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "COMPLETED");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.submitAnswers(sessionId, workspaceId,
                    List.of(new BriefingSessionService.AnswerInput(UUID.randomUUID(), "answer"))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("COMPLETED");
        }
    }

    // ============ completeBriefingSession ============

    @Nested
    @DisplayName("completeBriefingSession")
    class CompleteBriefingSession {

        @Test
        @DisplayName("should calculate 100% score when all required questions answered")
        void shouldCalculate100_whenAllRequiredAnswered() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            JpaServiceContextProfile profile = mockProfile(profileId, workspaceId, "SOCIAL_MEDIA");
            UUID q1 = UUID.randomUUID();
            UUID q2 = UUID.randomUUID();

            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.of(profile));
            when(questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profileId))
                    .thenReturn(List.of(
                            mockQuestion(q1, profileId, "Q1", 1, true),
                            mockQuestion(q2, profileId, "Q2", 2, true)
                    ));
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, q1)).thenReturn(true);
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, q2)).thenReturn(true);
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BriefingSessionService.CompletionResult result =
                    service.completeBriefingSession(sessionId, workspaceId);

            assertThat(result.completenessScore()).isEqualTo(100);
            assertThat(result.status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should calculate partial score when some required questions unanswered")
        void shouldCalculatePartialScore_whenSomeRequiredUnanswered() {
            // 8 required questions, 6 answered → 75%
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            JpaServiceContextProfile profile = mockProfile(profileId, workspaceId, "SOCIAL_MEDIA");

            List<UUID> questionIds = List.of(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()
            );
            List<JpaServiceContextQuestion> questions = new java.util.ArrayList<>();
            for (int i = 0; i < 8; i++) {
                questions.add(mockQuestion(questionIds.get(i), profileId, "Q" + i, i + 1, true));
            }

            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.of(profile));
            when(questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profileId))
                    .thenReturn(questions);

            // 6 of 8 answered
            for (int i = 0; i < 6; i++) {
                when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, questionIds.get(i)))
                        .thenReturn(true);
            }
            for (int i = 6; i < 8; i++) {
                when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, questionIds.get(i)))
                        .thenReturn(false);
            }
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BriefingSessionService.CompletionResult result =
                    service.completeBriefingSession(sessionId, workspaceId);

            assertThat(result.completenessScore()).isEqualTo(75);
        }

        @Test
        @DisplayName("should return 100% score when no profile exists (nothing required)")
        void shouldReturn100_whenNoProfileExists() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.empty());
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BriefingSessionService.CompletionResult result =
                    service.completeBriefingSession(sessionId, workspaceId);

            assertThat(result.completenessScore()).isEqualTo(100);
        }

        @Test
        @DisplayName("should throw IllegalStateException when session is not IN_PROGRESS")
        void shouldThrow_whenAlreadyCompleted() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "COMPLETED");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            assertThatThrownBy(() -> service.completeBriefingSession(sessionId, workspaceId))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("should persist COMPLETED status after completion")
        void shouldPersistCompletedStatus() {
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "SOCIAL_MEDIA"))
                    .thenReturn(Optional.empty());
            when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.completeBriefingSession(sessionId, workspaceId);

            verify(sessionRepo).save(argThat(s -> "COMPLETED".equals(s.getStatus())));
        }
    }

    // ============ Public token access ============

    @Nested
    @DisplayName("public token access")
    class PublicTokenAccess {

        @Test
        @DisplayName("should return session by valid public token")
        void shouldReturnSession_byValidToken() {
            String token = UUID.randomUUID().toString();
            JpaBriefingSession session = mockSession(sessionId, workspaceId, clientId, "SOCIAL_MEDIA", "IN_PROGRESS");
            session = spy(session);
            when(session.getPublicToken()).thenReturn(token);
            when(sessionRepo.findByPublicToken(token)).thenReturn(Optional.of(session));

            JpaBriefingSession result = service.getByPublicToken(token);

            assertThat(result.getId()).isEqualTo(sessionId);
        }

        @Test
        @DisplayName("should throw BriefingNotFoundException for unknown token")
        void shouldThrow_forUnknownToken() {
            when(sessionRepo.findByPublicToken(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByPublicToken("invalid-token"))
                    .isInstanceOf(BriefingNotFoundException.class);
        }
    }

    // ============ Completeness score unit tests ============

    @Nested
    @DisplayName("calculateCompletenessScore")
    class CalculateCompletenessScore {

        @Test
        @DisplayName("score = 0 when 0 of N required questions answered")
        void score0_whenNoneAnswered() {
            JpaServiceContextProfile profile = mockProfile(profileId, workspaceId, "LANDING_PAGE");
            UUID q1 = UUID.randomUUID();
            UUID q2 = UUID.randomUUID();

            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "LANDING_PAGE"))
                    .thenReturn(Optional.of(profile));
            when(questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profileId))
                    .thenReturn(List.of(
                            mockQuestion(q1, profileId, "Q1", 1, true),
                            mockQuestion(q2, profileId, "Q2", 2, true)
                    ));
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, q1)).thenReturn(false);
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, q2)).thenReturn(false);

            int score = service.calculateCompletenessScore(sessionId, workspaceId, "LANDING_PAGE");

            assertThat(score).isEqualTo(0);
        }

        @Test
        @DisplayName("optional questions do not affect score")
        void optionalQuestionsDoNotAffectScore() {
            JpaServiceContextProfile profile = mockProfile(profileId, workspaceId, "LANDING_PAGE");
            UUID required = UUID.randomUUID();
            UUID optional = UUID.randomUUID();

            when(profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, "LANDING_PAGE"))
                    .thenReturn(Optional.of(profile));
            when(questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profileId))
                    .thenReturn(List.of(
                            mockQuestion(required, profileId, "Required Q", 1, true),
                            mockQuestion(optional, profileId, "Optional Q", 2, false)
                    ));
            when(answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, required)).thenReturn(true);

            int score = service.calculateCompletenessScore(sessionId, workspaceId, "LANDING_PAGE");

            assertThat(score).isEqualTo(100);
        }
    }

    // ============ Test helpers ============

    private JpaProposal mockProposal(UUID id, UUID wkspId, UUID cId, UUID briefingId) {
        JpaProposal p = mock(JpaProposal.class);
        when(p.getId()).thenReturn(id);
        when(p.getWorkspaceId()).thenReturn(wkspId);
        when(p.getClientId()).thenReturn(cId);
        when(p.getBriefingId()).thenReturn(briefingId);
        return p;
    }

    private JpaBriefingSession mockSession(UUID id, UUID wkspId, UUID cId, String serviceType, String status) {
        JpaBriefingSession s = new JpaBriefingSession(
                id, wkspId, cId, serviceType, status,
                UUID.randomUUID().toString(), null, null, null,
                Instant.now(), Instant.now()
        );
        return s;
    }

    private JpaServiceContextProfile mockProfile(UUID id, UUID wkspId, String serviceType) {
        return new JpaServiceContextProfile(
                id, wkspId, serviceType, "Default Profile", null, null, null, null, null,
                true, Instant.now(), Instant.now()
        );
    }

    private JpaServiceContextQuestion mockQuestion(UUID id, UUID profileId, String text, int order, boolean required) {
        return new JpaServiceContextQuestion(id, profileId, text, "OPEN_ENDED", order, required, Instant.now());
    }
}
