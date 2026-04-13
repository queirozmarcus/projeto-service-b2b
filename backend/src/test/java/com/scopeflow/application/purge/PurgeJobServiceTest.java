package com.scopeflow.application.purge;

import com.scopeflow.adapter.out.persistence.briefing.JpaAIGenerationSpringRepository;
import com.scopeflow.application.idempotency.IdempotencyRepository;
import com.scopeflow.application.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurgeJobServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private JpaAIGenerationSpringRepository aiGenerationRepository;

    private PurgeJobService purgeJobService;

    @BeforeEach
    void setUp() {
        purgeJobService = new PurgeJobService(outboxEventRepository, idempotencyRepository, aiGenerationRepository);
        ReflectionTestUtils.setField(purgeJobService, "purgeEnabled", true);
        ReflectionTestUtils.setField(purgeJobService, "outboxRetentionDays", 7);
        ReflectionTestUtils.setField(purgeJobService, "idempotencyRetentionDays", 30);
        ReflectionTestUtils.setField(purgeJobService, "aiGenerationsRetentionDays", 90);
    }

    @Test
    void shouldDeletePublishedOutboxEvents_whenEnabled() {
        // Given
        when(outboxEventRepository.deletePublishedBefore(any(Instant.class))).thenReturn(5);

        // When
        purgeJobService.purgeOutboxEvents();

        // Then
        verify(outboxEventRepository, times(1)).deletePublishedBefore(any(Instant.class));
        verifyNoInteractions(idempotencyRepository, aiGenerationRepository);
    }

    @Test
    void shouldDeleteIdempotencyRecords_whenEnabled() {
        // Given
        when(idempotencyRepository.deleteBefore(any(Instant.class))).thenReturn(12);

        // When
        purgeJobService.purgeIdempotencyRecords();

        // Then
        verify(idempotencyRepository, times(1)).deleteBefore(any(Instant.class));
        verifyNoInteractions(outboxEventRepository, aiGenerationRepository);
    }

    @Test
    void shouldDeleteAiGenerations_whenEnabled() {
        // Given
        when(aiGenerationRepository.deleteCreatedBefore(any(Instant.class))).thenReturn(3);

        // When
        purgeJobService.purgeAiGenerations();

        // Then
        verify(aiGenerationRepository, times(1)).deleteCreatedBefore(any(Instant.class));
        verifyNoInteractions(outboxEventRepository, idempotencyRepository);
    }

    @Test
    void shouldSkipAllJobs_whenPurgeDisabled() {
        // Given
        ReflectionTestUtils.setField(purgeJobService, "purgeEnabled", false);

        // When
        purgeJobService.purgeOutboxEvents();
        purgeJobService.purgeIdempotencyRecords();
        purgeJobService.purgeAiGenerations();

        // Then
        verifyNoInteractions(outboxEventRepository, idempotencyRepository, aiGenerationRepository);
    }

    @Test
    void shouldUseCutoffBasedOnRetentionDays_outbox() {
        // Given
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        when(outboxEventRepository.deletePublishedBefore(cutoffCaptor.capture())).thenReturn(0);

        Instant before = Instant.now().minus(7, ChronoUnit.DAYS);

        // When
        purgeJobService.purgeOutboxEvents();

        // Then
        Instant capturedCutoff = cutoffCaptor.getValue();
        assertThat(capturedCutoff).isCloseTo(before, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldUseCutoffBasedOnRetentionDays_idempotency() {
        // Given
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        when(idempotencyRepository.deleteBefore(cutoffCaptor.capture())).thenReturn(0);

        Instant before = Instant.now().minus(30, ChronoUnit.DAYS);

        // When
        purgeJobService.purgeIdempotencyRecords();

        // Then
        Instant capturedCutoff = cutoffCaptor.getValue();
        assertThat(capturedCutoff).isCloseTo(before, within(1, ChronoUnit.SECONDS));
    }

    @Test
    void shouldUseCutoffBasedOnRetentionDays_aiGenerations() {
        // Given
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        when(aiGenerationRepository.deleteCreatedBefore(cutoffCaptor.capture())).thenReturn(0);

        Instant before = Instant.now().minus(90, ChronoUnit.DAYS);

        // When
        purgeJobService.purgeAiGenerations();

        // Then
        Instant capturedCutoff = cutoffCaptor.getValue();
        assertThat(capturedCutoff).isCloseTo(before, within(1, ChronoUnit.SECONDS));
    }
}
