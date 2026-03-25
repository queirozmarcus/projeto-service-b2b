package com.scopeflow.core.domain.briefing;

import com.scopeflow.core.domain.workspace.WorkspaceId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain service for Briefing aggregate.
 * Enforces all invariants and orchestrates state transitions.
 * No Spring dependencies: pure domain logic.
 */
public class BriefingService {
    private final BriefingSessionRepository sessionRepository;
    private final BriefingQuestionRepository questionRepository;
    private final BriefingAnswerRepository answerRepository;
    private final AIGenerationRepository aiGenerationRepository;

    public BriefingService(
            BriefingSessionRepository sessionRepository,
            BriefingQuestionRepository questionRepository,
            BriefingAnswerRepository answerRepository,
            AIGenerationRepository aiGenerationRepository
    ) {
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "sessionRepository cannot be null");
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository cannot be null");
        this.answerRepository = Objects.requireNonNull(answerRepository, "answerRepository cannot be null");
        this.aiGenerationRepository = Objects.requireNonNull(aiGenerationRepository, "aiGenerationRepository cannot be null");
    }

    /**
     * Find a briefing session by public token (client-facing, no auth).
     *
     * @param token the public token to look up
     * @return Optional with the session, or empty if not found
     */
    public Optional<BriefingSession> findByPublicToken(PublicToken token) {
        Objects.requireNonNull(token, "publicToken cannot be null");
        return sessionRepository.findByPublicToken(token);
    }

    /**
     * Start a new briefing session.
     *
     * Invariant: Only 1 active briefing per client per service type.
     *
     * @throws BriefingAlreadyInProgressException if active briefing exists
     */
    public BriefingInProgress startBriefing(
            WorkspaceId workspaceId,
            ClientId clientId,
            ServiceType serviceType
    ) throws BriefingAlreadyInProgressException {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        Objects.requireNonNull(clientId, "clientId cannot be null");
        Objects.requireNonNull(serviceType, "serviceType cannot be null");

        Optional<BriefingSession> active = sessionRepository.findActiveByClientAndService(clientId, serviceType);
        if (active.isPresent()) {
            throw new BriefingAlreadyInProgressException(
                    "Only 1 active briefing per client per service type. Client: " + clientId + ", Service: " + serviceType
            );
        }

        BriefingInProgress session = BriefingSession.startNew(workspaceId, clientId, serviceType);
        sessionRepository.save(session);

        return session;
    }

    /**
     * Get the next question for a briefing session.
     *
     * Invariant: Questions must be answered sequentially (no skip).
     *
     * @throws BriefingNotFoundException if session not found
     * @throws InvalidStateException if session not in progress
     */
    public BriefingQuestion getNextQuestion(BriefingSessionId sessionId)
            throws BriefingNotFoundException, InvalidStateException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        BriefingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        if (!(session instanceof BriefingInProgress)) {
            throw new InvalidStateException("Session not in progress. Current status: " + session.status());
        }

        // Calculate next step based on answers already submitted
        long answersCount = sessionRepository.countAnswers(sessionId);
        int nextStep = (int) (answersCount + 1);

        return questionRepository.findBySessionAndStep(sessionId, nextStep)
                .orElseThrow(() -> new InvalidStateException("No more questions available. Next step: " + nextStep));
    }

    /**
     * Submit a direct answer (without follow-up).
     *
     * Invariant: No empty answers.
     *
     * @throws InvalidAnswerException if answer invalid
     */
    public void submitDirectAnswer(
            BriefingSessionId sessionId,
            QuestionId questionId,
            AnswerText answerText,
            int qualityScore
    ) throws InvalidAnswerException, BriefingNotFoundException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(questionId, "questionId cannot be null");
        Objects.requireNonNull(answerText, "answerText cannot be null");

        // Verify session exists
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        AnswerId answerId = AnswerId.generate();
        AnsweredDirect answer = new AnsweredDirect(
                answerId,
                sessionId,
                questionId,
                answerText,
                java.time.Instant.now(),
                qualityScore
        );
        answerRepository.save(answer);
    }

    /**
     * Submit an answer with auto-generated follow-up question.
     *
     * Invariant: Max 1 follow-up per question.
     *
     * @throws MaxFollowupExceededException if follow-up limit exceeded
     */
    public void submitAnswerWithFollowup(
            BriefingSessionId sessionId,
            QuestionId questionId,
            AnswerText answerText,
            BriefingQuestion generatedFollowup,
            int confidenceScore
    ) throws MaxFollowupExceededException, BriefingNotFoundException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(questionId, "questionId cannot be null");
        Objects.requireNonNull(answerText, "answerText cannot be null");
        Objects.requireNonNull(generatedFollowup, "generatedFollowup cannot be null");

        // Verify session exists
        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        // Check follow-up limit
        long followupCount = answerRepository.countFollowupsByQuestion(questionId);
        if (followupCount >= 1) {
            throw new MaxFollowupExceededException(
                    "Max 1 follow-up per question. Question: " + questionId + " already has " + followupCount
            );
        }

        AnswerId answerId = AnswerId.generate();
        AnsweredWithFollowup answer = new AnsweredWithFollowup(
                answerId,
                sessionId,
                questionId,
                answerText,
                java.time.Instant.now(),
                generatedFollowup,
                confidenceScore
        );
        answerRepository.save(answer);

        // Save the generated follow-up question
        questionRepository.save(generatedFollowup);
    }

    /**
     * Detect gaps in the briefing.
     *
     * <p>Always returns a non-null GapAnalysis. A low score does not mean absence of
     * information — it is valid information that the briefing is incomplete.
     * Use {@link GapAnalysis#isEligibleForCompletion()} to check if score >= 80%.
     *
     * @return GapAnalysis with current score and identified gaps — never null
     * @throws BriefingNotFoundException if session not found
     */
    public GapAnalysis detectGaps(BriefingSessionId sessionId)
            throws BriefingNotFoundException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        List<BriefingAnswer> answers = answerRepository.findBySession(sessionId);

        // Simple gap detection: score = (answers / expected) * 100
        // In real implementation, this would use AI analysis
        int expectedAnswers = 10; // Placeholder
        int score = Math.min(100, (answers.size() * 100) / expectedAnswers);

        List<String> gaps = new ArrayList<>();
        int remaining = expectedAnswers - answers.size();
        if (remaining > 0) {
            gaps.add("Need " + remaining + " more answer" + (remaining == 1 ? "" : "s") + " to reach minimum threshold");
        }

        return new GapAnalysis(score, gaps);
    }

    /**
     * Complete the briefing (mark as ready for scope generation).
     *
     * Invariant: Completion score must be >= 80% and no critical gaps.
     *
     * @throws IncompleteGapsException if score < 80%
     * @throws BriefingNotFoundException if session not found
     * @throws BriefingAlreadyCompletedException if already completed
     */
    public BriefingCompleted completeBriefing(
            BriefingSessionId sessionId,
            CompletionScore score
    ) throws IncompleteGapsException, BriefingNotFoundException, BriefingAlreadyCompletedException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(score, "score cannot be null");

        if (score.score() < 80) {
            throw new IncompleteGapsException(
                    "Completion score < 80%. Current: " + score.score()
            );
        }

        BriefingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        if (!(session instanceof BriefingInProgress inProgress)) {
            if (session instanceof BriefingCompleted) {
                throw new BriefingAlreadyCompletedException("Session already completed");
            }
            throw new InvalidStateException("Session not in progress. Current status: " + session.status());
        }

        BriefingCompleted completed = inProgress.completeBriefing(score);
        sessionRepository.save(completed);

        return completed;
    }

    /**
     * Abandon the briefing.
     * Client can start a new briefing later.
     *
     * @throws BriefingNotFoundException if session not found
     * @throws BriefingAlreadyCompletedException if already completed
     */
    public BriefingAbandoned abandonBriefing(BriefingSessionId sessionId)
            throws BriefingNotFoundException, BriefingAlreadyCompletedException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");

        BriefingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        if (session instanceof BriefingCompleted) {
            throw new BriefingAlreadyCompletedException("Cannot abandon completed session");
        }

        if (!(session instanceof BriefingInProgress inProgress)) {
            throw new InvalidStateException("Session not in progress. Current status: " + session.status());
        }

        BriefingAbandoned abandoned = inProgress.abandon();
        sessionRepository.save(abandoned);

        return abandoned;
    }

    /**
     * Record an AI generation for audit trail.
     */
    public void recordAIGeneration(AIGeneration generation) {
        Objects.requireNonNull(generation, "generation cannot be null");
        aiGenerationRepository.save(generation);
    }

    // ============ Query methods (encapsulados — controllers delegam para cá) ============

    /**
     * Find a briefing session by ID and validate workspace ownership.
     *
     * @throws BriefingNotFoundException if session not found
     * @throws org.springframework.security.access.AccessDeniedException if session belongs to another workspace
     */
    public BriefingSession findByIdAndWorkspace(BriefingSessionId sessionId, WorkspaceId workspaceId)
            throws BriefingNotFoundException {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");

        BriefingSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new BriefingNotFoundException("Briefing session not found: " + sessionId));

        if (!session.getWorkspaceId().equals(workspaceId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Briefing does not belong to authenticated workspace"
            );
        }

        return session;
    }

    /**
     * Find all questions for a session.
     */
    public List<BriefingQuestion> findQuestions(BriefingSessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        return questionRepository.findBySession(sessionId);
    }

    /**
     * Find all answers for a session.
     */
    public List<BriefingAnswer> findAnswers(BriefingSessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId cannot be null");
        return answerRepository.findBySession(sessionId);
    }

    /**
     * Find paginated briefings for a workspace with optional filters.
     */
    public List<BriefingSession> findByWorkspaceAndStatus(WorkspaceId workspaceId, String status) {
        Objects.requireNonNull(workspaceId, "workspaceId cannot be null");
        return sessionRepository.findByWorkspaceAndStatus(workspaceId, status);
    }
}
