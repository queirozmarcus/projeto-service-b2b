package com.scopeflow.adapter.in.web.briefing.fixtures;

import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswer;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswerSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSessionSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfile;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfileSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestion;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestionSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposal;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalSpringRepository;
import com.scopeflow.core.application.briefing.BriefingSessionService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Centralized test fixtures for BriefingSession integration tests.
 *
 * Provides factory methods that create and persist the entities needed
 * for briefing session tests: proposals, sessions, profiles, and questions.
 * All factories accept explicit IDs for predictable assertions.
 */
public final class BriefingSessionTestFixtures {

    private BriefingSessionTestFixtures() {}

    // ============ Briefing Session ============

    /**
     * Create and persist an IN_PROGRESS briefing session for a workspace.
     */
    public static JpaBriefingSession createInProgressSession(
            UUID workspaceId,
            JpaBriefingSessionSpringRepository sessionRepo
    ) {
        Instant now = Instant.now();
        JpaBriefingSession session = new JpaBriefingSession(
                UUID.randomUUID(),
                workspaceId,
                UUID.randomUUID(),   // clientId
                "SOCIAL_MEDIA",
                "IN_PROGRESS",
                UUID.randomUUID().toString(), // publicToken
                null,
                null,
                null,
                now,
                now
        );
        return sessionRepo.save(session);
    }

    /**
     * Create and persist a COMPLETED briefing session.
     * Used as the linked briefing required by the proposals FK constraint.
     */
    public static JpaBriefingSession createCompletedSession(
            UUID workspaceId,
            JpaBriefingSessionSpringRepository sessionRepo
    ) {
        Instant now = Instant.now();
        JpaBriefingSession session = new JpaBriefingSession(
                UUID.randomUUID(),
                workspaceId,
                UUID.randomUUID(),
                "SOCIAL_MEDIA",
                "COMPLETED",
                UUID.randomUUID().toString(),
                95,
                null,
                null,
                now,
                now
        );
        return sessionRepo.save(session);
    }

    // ============ Proposal ============

    /**
     * Create and persist a DRAFT proposal linked to a briefing session.
     */
    public static JpaProposal createDraftProposal(
            UUID workspaceId,
            UUID briefingSessionId,
            JpaProposalSpringRepository proposalRepo
    ) {
        Instant now = Instant.now();
        JpaProposal proposal = new JpaProposal(
                UUID.randomUUID(),
                workspaceId,
                UUID.randomUUID(),  // clientId
                briefingSessionId,
                "Test Proposal",
                "DRAFT",
                null,
                now,
                now
        );
        return proposalRepo.save(proposal);
    }

    // ============ ServiceContextProfile ============

    /**
     * Create and persist an active ServiceContextProfile with N questions.
     *
     * @param workspaceId       workspace owner
     * @param serviceType       e.g. "SOCIAL_MEDIA", "LANDING_PAGE"
     * @param totalQuestions    total number of questions to create
     * @param requiredQuestions how many of those questions have is_required = true (first N)
     * @param profileRepo       repository for profiles
     * @param questionRepo      repository for questions
     * @return the created profile (questions are side-effect persisted)
     */
    public static JpaServiceContextProfile createProfileWithQuestions(
            UUID workspaceId,
            String serviceType,
            int totalQuestions,
            int requiredQuestions,
            JpaServiceContextProfileSpringRepository profileRepo,
            JpaServiceContextQuestionSpringRepository questionRepo
    ) {
        Instant now = Instant.now();
        JpaServiceContextProfile profile = new JpaServiceContextProfile(
                UUID.randomUUID(),
                workspaceId,
                serviceType,
                "Test Profile for " + serviceType,
                null, null, null, null, null,
                true,   // active
                now,
                now
        );
        profile = profileRepo.save(profile);

        for (int i = 1; i <= totalQuestions; i++) {
            boolean required = (i <= requiredQuestions);
            JpaServiceContextQuestion question = new JpaServiceContextQuestion(
                    UUID.randomUUID(),
                    profile.getId(),
                    "Question " + i + " for " + serviceType,
                    "OPEN_ENDED",
                    i,
                    required,
                    now
            );
            questionRepo.save(question);
        }

        return profile;
    }

    /**
     * Convenience: create profile with all questions required.
     */
    public static JpaServiceContextProfile createProfileWithAllRequired(
            UUID workspaceId,
            String serviceType,
            int questionCount,
            JpaServiceContextProfileSpringRepository profileRepo,
            JpaServiceContextQuestionSpringRepository questionRepo
    ) {
        return createProfileWithQuestions(
                workspaceId, serviceType, questionCount, questionCount,
                profileRepo, questionRepo
        );
    }

    /**
     * Create a single question attached to a profile.
     */
    public static JpaServiceContextQuestion createQuestion(
            UUID profileId,
            String text,
            int orderIndex,
            boolean required,
            JpaServiceContextQuestionSpringRepository questionRepo
    ) {
        JpaServiceContextQuestion question = new JpaServiceContextQuestion(
                UUID.randomUUID(),
                profileId,
                text,
                "OPEN_ENDED",
                orderIndex,
                required,
                Instant.now()
        );
        return questionRepo.save(question);
    }

    // ============ Answers ============

    /**
     * Submit answers for all provided questionIds, linked to the given session.
     */
    public static void submitAnswersForQuestions(
            UUID sessionId,
            List<UUID> questionIds,
            JpaBriefingAnswerSpringRepository answerRepo
    ) {
        Instant now = Instant.now();
        for (UUID questionId : questionIds) {
            JpaBriefingAnswer answer = new JpaBriefingAnswer(
                    UUID.randomUUID(),
                    sessionId,
                    questionId,
                    "Test answer for " + questionId,
                    null, null, null,
                    now
            );
            answerRepo.save(answer);
        }
    }

    // ============ AnswerInput factories ============

    /**
     * Build a list of AnswerInput values for the given question IDs.
     * Useful for submitting via API in integration tests.
     */
    public static List<BriefingSessionService.AnswerInput> sampleAnswerInputs(List<UUID> questionIds) {
        List<BriefingSessionService.AnswerInput> inputs = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            inputs.add(new BriefingSessionService.AnswerInput(
                    questionIds.get(i),
                    "Answer " + (i + 1) + " for question " + questionIds.get(i)
            ));
        }
        return inputs;
    }

    /**
     * Serialize a list of AnswerInput as JSON body string for MockMvc requests.
     *
     * Example output:
     * {"answers":[{"questionId":"...","answerText":"..."}]}
     */
    public static String answersJson(List<UUID> questionIds) {
        StringBuilder sb = new StringBuilder("{\"answers\":[");
        for (int i = 0; i < questionIds.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"questionId\":\"")
              .append(questionIds.get(i))
              .append("\",\"answerText\":\"Answer for question ")
              .append(i + 1)
              .append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Single-answer JSON body.
     */
    public static String singleAnswerJson(UUID questionId, String answerText) {
        return """
                {"answers":[{"questionId":"%s","answerText":"%s"}]}
                """.formatted(questionId, answerText).strip();
    }
}
