package com.scopeflow.application.purge;

import com.scopeflow.adapter.out.persistence.briefing.JpaAIGenerationSpringRepository;
import com.scopeflow.application.idempotency.IdempotencyRepository;
import com.scopeflow.application.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Scheduled purge jobs for time-bounded tables.
 *
 * Three independent jobs run daily at staggered times to avoid I/O spikes:
 *   - 02:00 — outbox_event    (published events, default 7-day retention)
 *   - 02:15 — idempotency_record (default 30-day retention)
 *   - 02:30 — ai_generations  (default 90-day retention)
 *
 * Each job is independently transactional and skippable via app.purge.enabled=false.
 * Retention windows are configurable via environment variables (see application.yml).
 */
@Component
public class PurgeJobService {

    private static final Logger log = LoggerFactory.getLogger(PurgeJobService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final JpaAIGenerationSpringRepository aiGenerationRepository;

    @Value("${app.purge.enabled:true}")
    private boolean purgeEnabled;

    @Value("${app.purge.outbox-retention-days:7}")
    private int outboxRetentionDays;

    @Value("${app.purge.idempotency-retention-days:30}")
    private int idempotencyRetentionDays;

    @Value("${app.purge.ai-generations-retention-days:90}")
    private int aiGenerationsRetentionDays;

    public PurgeJobService(
            OutboxEventRepository outboxEventRepository,
            IdempotencyRepository idempotencyRepository,
            JpaAIGenerationSpringRepository aiGenerationRepository) {
        this.outboxEventRepository = outboxEventRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.aiGenerationRepository = aiGenerationRepository;
    }

    /**
     * Purge published outbox events older than the retention window.
     * Only deletes events where published_at IS NOT NULL — unpublished events are never touched.
     * Runs daily at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeOutboxEvents() {
        if (!purgeEnabled) {
            log.info("Purge disabled — skipping outbox events purge");
            return;
        }

        Instant cutoff = Instant.now().minus(outboxRetentionDays, ChronoUnit.DAYS);
        int deleted = outboxEventRepository.deletePublishedBefore(cutoff);
        log.info("Purge completed: deleted {} outbox events (cutoff={})", deleted, cutoff);
    }

    /**
     * Purge idempotency records older than the retention window.
     * Dedup window is considered closed after the retention period.
     * Runs daily at 02:15.
     */
    @Scheduled(cron = "0 15 2 * * *")
    @Transactional
    public void purgeIdempotencyRecords() {
        if (!purgeEnabled) {
            log.info("Purge disabled — skipping idempotency records purge");
            return;
        }

        Instant cutoff = Instant.now().minus(idempotencyRetentionDays, ChronoUnit.DAYS);
        int deleted = idempotencyRepository.deleteBefore(cutoff);
        log.info("Purge completed: deleted {} idempotency records (cutoff={})", deleted, cutoff);
    }

    /**
     * Purge AI generation records older than the retention window.
     * Bulk DELETE via JPQL — does not load entities into memory.
     * Runs daily at 02:30.
     */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void purgeAiGenerations() {
        if (!purgeEnabled) {
            log.info("Purge disabled — skipping AI generations purge");
            return;
        }

        Instant cutoff = Instant.now().minus(aiGenerationsRetentionDays, ChronoUnit.DAYS);
        int deleted = aiGenerationRepository.deleteCreatedBefore(cutoff);
        log.info("Purge completed: deleted {} AI generations (cutoff={})", deleted, cutoff);
    }
}
