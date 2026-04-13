package com.scopeflow.application.listener;

import com.scopeflow.application.idempotency.IdempotencyService;
import com.scopeflow.core.domain.briefing.event.BriefingCompletedEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event Listener: BriefingCompletedEvent → Fallback Question Generation.
 *
 * Responsibility:
 * - Listens for BriefingCompletedEvent on RabbitMQ queue: briefing.completed
 * - Fallback: if questions weren't generated during session (edge case),
 *   regenerate them now before scope generation
 * - Idempotent: won't regenerate questions multiple times
 *
 * When is this needed?
 * - Session completed with score >= 80% but no questions generated?
 *   Rare, but handled by this listener
 * - Normal flow: questions generated in-session (faster)
 * - Edge case: service failure during session → regenerate here
 *
 * Idempotency Key: "briefing-{sessionId}"
 * - Unique per briefing session
 * - Only one event per completed session
 *
 * Error Handling:
 * - IA call failures: RabbitMQ retry
 * - After 3 retries: DLQ for manual inspection
 * - If fallback fails: scope generation may use default questions
 *
 * Performance:
 * - Lightweight: async listener
 * - IA call: ~2-3 seconds (OpenAI latency)
 * - Timeout: 30s
 */
@Component
public class BriefingCompletedListener {
    private static final Logger logger = LoggerFactory.getLogger(BriefingCompletedListener.class);
    private static final String LISTENER_ID = "briefing-completed-listener";

    private final IdempotencyService idempotencyService;
    // private final StartBriefingSessionService startService;  // TODO: For question generation
    // private final AIAssistantPort aiAssistant;              // TODO: For fallback IA

    public BriefingCompletedListener(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    /**
     * Handle BriefingCompletedEvent.
     *
     * @RabbitListener converts AMQP message from queue "briefing.completed"
     * to BriefingCompletedEvent domain object.
     *
     * @param event the BriefingCompletedEvent from RabbitMQ
     */
    @RabbitListener(queues = "briefing.completed")
    @Transactional
    public void onBriefingCompleted(BriefingCompletedEvent event) {
        UUID sessionId = event.sessionId();
        String idempotencyKey = "briefing-" + sessionId;

        // Step 1: Check if already processed (idempotent)
        if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
            logger.info(
                    "BriefingCompletedEvent already processed (idempotent check), skipping. sessionId={}",
                    sessionId
            );
            return;
        }

        try {
            // Step 2: Fallback question generation (Phase 2.3 integration)
            // Scenario: session completed but questions not generated during flow
            // TODO: Call IA to generate questions:
            // List<BriefingQuestion> questions = aiAssistant.generateQuestions(
            //     event.sessionId(),
            //     promptVersion="briefing_questions_v1"
            // );

            logger.info(
                    "Fallback: questions would be generated for briefing session: {}",
                    sessionId
            );

            // Step 3: Mark as processed
            idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey);

            logger.info(
                    "BriefingCompletedEvent processed successfully. sessionId={}",
                    sessionId
            );

        } catch (Exception e) {
            // Log error; RabbitMQ will retry
            logger.error(
                    "Failed to process BriefingCompletedEvent. sessionId={}, will retry",
                    sessionId,
                    e
            );
            // Re-throw to trigger RabbitMQ retry
            throw new RuntimeException("Failed to generate fallback questions", e);
        }
    }
}
