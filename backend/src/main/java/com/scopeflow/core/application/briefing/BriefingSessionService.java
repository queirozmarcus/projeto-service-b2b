package com.scopeflow.core.application.briefing;

import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswer;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingAnswerSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSessionSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfile;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextProfileSpringRepository;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestion;
import com.scopeflow.adapter.out.persistence.briefing.JpaServiceContextQuestionSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalSpringRepository;
import com.scopeflow.core.domain.briefing.BriefingNotFoundException;
import com.scopeflow.core.domain.proposal.ProposalNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service for BriefingSession discovery flow (Sprint 6 Task 3).
 *
 * <p>Orchestrates:
 * <ul>
 *   <li>Creating a BriefingSession linked to a Proposal (inheriting client+serviceType)</li>
 *   <li>Listing questions from the ServiceContextProfile for that service type</li>
 *   <li>Submitting answers (batch) — saved as BriefingAnswer rows</li>
 *   <li>Completing the session with a calculated completeness score</li>
 *   <li>Public token access for unauthenticated client responses</li>
 * </ul>
 *
 * <p>Workspace isolation: all operations validate that the requested proposal belongs to
 * the authenticated workspace before proceeding.
 */
@Service
@Transactional(readOnly = true)
public class BriefingSessionService {

    private static final Logger log = LoggerFactory.getLogger(BriefingSessionService.class);

    private final JpaBriefingSessionSpringRepository sessionRepo;
    private final JpaBriefingAnswerSpringRepository answerRepo;
    private final JpaServiceContextProfileSpringRepository profileRepo;
    private final JpaServiceContextQuestionSpringRepository questionRepo;
    private final JpaProposalSpringRepository proposalRepo;

    public BriefingSessionService(
            JpaBriefingSessionSpringRepository sessionRepo,
            JpaBriefingAnswerSpringRepository answerRepo,
            JpaServiceContextProfileSpringRepository profileRepo,
            JpaServiceContextQuestionSpringRepository questionRepo,
            JpaProposalSpringRepository proposalRepo
    ) {
        this.sessionRepo = sessionRepo;
        this.answerRepo = answerRepo;
        this.profileRepo = profileRepo;
        this.questionRepo = questionRepo;
        this.proposalRepo = proposalRepo;
    }

    // ============ Commands ============

    /**
     * Create a new BriefingSession for a Proposal.
     *
     * <p>Inherits clientId and serviceType from the briefing_session already linked
     * to the proposal (via proposals.briefing_id). If the proposal has no linked
     * briefing session, falls back to a new orphan session using the workspace.
     *
     * <p>Invariant: workspace isolation — proposalId must belong to workspaceId.
     *
     * @throws ProposalNotFoundException if proposal does not exist or is deleted
     * @throws AccessDeniedException     if proposal does not belong to workspaceId
     */
    @Transactional
    public JpaBriefingSession createBriefingSession(UUID proposalId, UUID workspaceId) {
        var proposal = proposalRepo.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException("Proposal not found: " + proposalId));

        if (!proposal.getWorkspaceId().equals(workspaceId)) {
            throw new AccessDeniedException("Proposal does not belong to the authenticated workspace");
        }

        // Determine clientId and serviceType from the existing linked briefing session,
        // or fall back to sensible defaults derived from the proposal itself.
        UUID clientId = proposal.getClientId();
        String serviceType = resolveServiceType(proposal.getBriefingId());

        String publicToken = UUID.randomUUID().toString();
        Instant now = Instant.now();

        JpaBriefingSession session = new JpaBriefingSession(
                UUID.randomUUID(),
                workspaceId,
                clientId,
                serviceType,
                "IN_PROGRESS",
                publicToken,
                null,
                null,
                null,
                now,
                now
        );

        JpaBriefingSession saved = sessionRepo.save(session);
        log.info("BriefingSession created: sessionId={}, proposalId={}, workspaceId={}, serviceType={}",
                saved.getId(), proposalId, workspaceId, serviceType);
        return saved;
    }

    /**
     * Retrieve a session, validating workspace ownership via the session's workspaceId.
     *
     * @throws BriefingNotFoundException if session not found
     * @throws AccessDeniedException     if session belongs to a different workspace
     */
    public JpaBriefingSession getBriefingSession(UUID sessionId, UUID workspaceId) {
        JpaBriefingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("BriefingSession not found: " + sessionId));

        if (!session.getWorkspaceId().equals(workspaceId)) {
            throw new AccessDeniedException("BriefingSession does not belong to the authenticated workspace");
        }
        return session;
    }

    /**
     * List template questions from the ServiceContextProfile for this session's serviceType.
     *
     * <p>If no ServiceContextProfile exists for the workspace+serviceType, returns an empty list.
     * This is intentional: profiles are optional (workspace may not have configured them yet).
     *
     * @throws BriefingNotFoundException if session not found
     * @throws AccessDeniedException     if session belongs to a different workspace
     */
    public List<JpaServiceContextQuestion> getQuestions(UUID sessionId, UUID workspaceId) {
        JpaBriefingSession session = getBriefingSession(sessionId, workspaceId);

        Optional<JpaServiceContextProfile> profile = profileRepo
                .findActiveByWorkspaceAndServiceType(workspaceId, session.getServiceType());

        if (profile.isEmpty()) {
            log.debug("No ServiceContextProfile found for workspace={}, serviceType={} — returning empty questions",
                    workspaceId, session.getServiceType());
            return List.of();
        }

        return questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profile.get().getId());
    }

    /**
     * Submit multiple answers to a briefing session in a single transaction.
     *
     * <p>Idempotency: if an answer for the same (sessionId, questionId) already exists,
     * it is silently skipped (unique constraint enforced by DB).
     *
     * @throws BriefingNotFoundException if session not found
     * @throws AccessDeniedException     if session belongs to a different workspace
     * @throws IllegalStateException     if session is not IN_PROGRESS
     */
    @Transactional
    public void submitAnswers(UUID sessionId, UUID workspaceId, List<AnswerInput> answers) {
        JpaBriefingSession session = getBriefingSession(sessionId, workspaceId);

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException(
                    "Cannot submit answers to a session with status: " + session.getStatus());
        }

        Instant now = Instant.now();
        for (AnswerInput input : answers) {
            // Skip if answer already exists for this question (idempotent)
            boolean alreadyAnswered = answerRepo.existsByBriefingSessionIdAndQuestionId(
                    sessionId, input.questionId());
            if (alreadyAnswered) {
                log.debug("Answer already exists for sessionId={}, questionId={} — skipping",
                        sessionId, input.questionId());
                continue;
            }
            JpaBriefingAnswer answer = new JpaBriefingAnswer(
                    UUID.randomUUID(),
                    sessionId,
                    input.questionId(),
                    input.answerText(),
                    null,
                    null,
                    null,
                    now
            );
            answerRepo.save(answer);
        }
        log.info("Answers submitted: sessionId={}, count={}", sessionId, answers.size());
    }

    /**
     * Complete the briefing session and calculate the completeness score.
     *
     * <p>Score calculation:
     * <pre>
     *   required = questions with is_required = true
     *   answered = required questions that have an answer
     *   score = (answered / required) * 100   (0 if no required questions exist → 100%)
     * </pre>
     *
     * @throws BriefingNotFoundException if session not found
     * @throws AccessDeniedException     if session belongs to a different workspace
     * @throws IllegalStateException     if session is not IN_PROGRESS
     */
    @Transactional
    public CompletionResult completeBriefingSession(UUID sessionId, UUID workspaceId) {
        JpaBriefingSession session = getBriefingSession(sessionId, workspaceId);

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException(
                    "Cannot complete a session with status: " + session.getStatus());
        }

        int score = calculateCompletenessScore(sessionId, session.getWorkspaceId(), session.getServiceType());

        // Persist the completed state via a new JPA entity (immutable entity pattern — replace rather than mutate)
        JpaBriefingSession completed = new JpaBriefingSession(
                session.getId(),
                session.getWorkspaceId(),
                session.getClientId(),
                session.getServiceType(),
                "COMPLETED",
                session.getPublicToken(),
                score,
                session.getAiAnalysis(),
                session.getAbandonedReason(),
                session.getCreatedAt(),
                Instant.now()
        );
        sessionRepo.save(completed);

        log.info("BriefingSession completed: sessionId={}, score={}", sessionId, score);
        return new CompletionResult(score, "COMPLETED");
    }

    /**
     * Find session by public token — for unauthenticated client access.
     *
     * @throws BriefingNotFoundException if token is invalid or session does not exist
     */
    public JpaBriefingSession getByPublicToken(String token) {
        return sessionRepo.findByPublicToken(token)
                .orElseThrow(() -> new BriefingNotFoundException(
                        "BriefingSession not found for token: " + token));
    }

    /**
     * List template questions by public token — for unauthenticated client access.
     *
     * @throws BriefingNotFoundException if token is invalid
     */
    public List<JpaServiceContextQuestion> getQuestionsByPublicToken(String token) {
        JpaBriefingSession session = getByPublicToken(token);

        Optional<JpaServiceContextProfile> profile = profileRepo
                .findActiveByWorkspaceAndServiceType(session.getWorkspaceId(), session.getServiceType());

        return profile
                .map(p -> questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(p.getId()))
                .orElse(List.of());
    }

    /**
     * Submit answers by public token — no authentication required.
     *
     * @throws BriefingNotFoundException if token invalid
     * @throws IllegalStateException     if session is not IN_PROGRESS
     */
    @Transactional
    public void submitAnswersByPublicToken(String token, List<AnswerInput> answers) {
        JpaBriefingSession session = getByPublicToken(token);

        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new IllegalStateException(
                    "Cannot submit answers to a session with status: " + session.getStatus());
        }

        Instant now = Instant.now();
        for (AnswerInput input : answers) {
            boolean alreadyAnswered = answerRepo.existsByBriefingSessionIdAndQuestionId(
                    session.getId(), input.questionId());
            if (alreadyAnswered) {
                continue;
            }
            answerRepo.save(new JpaBriefingAnswer(
                    UUID.randomUUID(),
                    session.getId(),
                    input.questionId(),
                    input.answerText(),
                    null,
                    null,
                    null,
                    now
            ));
        }
        log.info("Public answers submitted: token={}, count={}", token, answers.size());
    }

    // ============ Internal helpers ============

    /**
     * Calculate completeness score based on required questions vs answered.
     *
     * <p>Formula: (answeredRequired / totalRequired) * 100
     * If no required questions exist, score = 100.
     */
    int calculateCompletenessScore(UUID sessionId, UUID workspaceId, String serviceType) {
        Optional<JpaServiceContextProfile> profile =
                profileRepo.findActiveByWorkspaceAndServiceType(workspaceId, serviceType);

        if (profile.isEmpty()) {
            // No profile configured — consider complete (no required questions to verify)
            return 100;
        }

        List<JpaServiceContextQuestion> questions =
                questionRepo.findByServiceContextProfileIdOrderByOrderIndexAsc(profile.get().getId());

        List<UUID> requiredQuestionIds = questions.stream()
                .filter(JpaServiceContextQuestion::isRequired)
                .map(JpaServiceContextQuestion::getId)
                .toList();

        if (requiredQuestionIds.isEmpty()) {
            return 100;
        }

        long answeredCount = requiredQuestionIds.stream()
                .filter(qId -> answerRepo.existsByBriefingSessionIdAndQuestionId(sessionId, qId))
                .count();

        return (int) ((answeredCount * 100) / requiredQuestionIds.size());
    }

    /**
     * Resolve serviceType from the briefing session already linked to the proposal.
     * Falls back to 'SOCIAL_MEDIA' (most common) if the linked session is not found.
     */
    private String resolveServiceType(UUID linkedBriefingId) {
        if (linkedBriefingId == null) {
            return "SOCIAL_MEDIA";
        }
        return sessionRepo.findById(linkedBriefingId)
                .map(JpaBriefingSession::getServiceType)
                .orElse("SOCIAL_MEDIA");
    }

    // ============ Value objects ============

    /**
     * Input for a single answer submission.
     */
    public record AnswerInput(UUID questionId, String answerText) {}

    /**
     * Result of completing a briefing session.
     */
    public record CompletionResult(int completenessScore, String status) {}
}
