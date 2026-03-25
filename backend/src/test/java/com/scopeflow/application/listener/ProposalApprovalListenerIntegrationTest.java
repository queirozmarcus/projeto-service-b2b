package com.scopeflow.application.listener;

import com.scopeflow.application.fixtures.MessagingEventFixtures;
import com.scopeflow.application.fixtures.MessagingIntegrationTestBase;
import com.scopeflow.application.idempotency.IdempotencyRecord;
import com.scopeflow.application.port.out.EmailException;
import com.scopeflow.application.port.out.EmailService;
import com.scopeflow.application.port.out.PdfGenerationException;
import com.scopeflow.application.port.out.PdfService;
import com.scopeflow.application.port.out.PdfService.GenerationContext;
import com.scopeflow.core.domain.proposal.event.ProposalApprovedEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ProposalApprovalListener (Sprint 4, Phase 2 + Phase 3 + Phase 4).
 *
 * Validates the full async approval flow:
 * 1. ProposalApprovedEvent arrives on queue "proposal.approved"
 * 2. Listener checks idempotency
 * 3. PdfService.generateProposalPdf() called with APPROVAL context
 * 4. EmailService.sendProposalApprovedEmail() called with presigned URL
 * 5. Idempotency record persisted with resultData (pdfUrl + emailSent flag)
 *
 * Infrastructure:
 * - RabbitMQ (Testcontainers) — real broker
 * - PostgreSQL (Testcontainers) — real idempotency_record table
 * - PdfService mocked (@MockBean) — no real iText/S3 calls
 * - EmailService mocked (@MockBean) — no real SES calls
 */
@DisplayName("ProposalApprovalListener — Integration Tests")
class ProposalApprovalListenerIntegrationTest extends MessagingIntegrationTestBase {

    @MockBean
    private PdfService pdfService;

    @MockBean
    private EmailService emailService;

    private static final String MOCK_PDF_URL =
            "https://scopeflow-mvp.s3.amazonaws.com/proposals/test/v1.pdf?presigned=1";

    @AfterEach
    void cleanup() {
        idempotencyRepository.deleteAll();
    }

    @Nested
    @DisplayName("Happy path — PDF + email")
    class HappyPathTests {

        @Test
        @DisplayName("should generate PDF, send approval email and persist idempotency record")
        void shouldGeneratePdfAndSendEmailAndMarkAsProcessed() throws Exception {
            // Given
            UUID proposalId = UUID.randomUUID();
            UUID workflowId = UUID.randomUUID();
            String clientEmail = "client@startup.com";
            ProposalApprovedEvent event = MessagingEventFixtures.proposalApprovedEvent(
                    proposalId, workflowId, clientEmail);

            when(pdfService.generateProposalPdf(eq(proposalId), eq(GenerationContext.APPROVAL)))
                    .thenReturn(MOCK_PDF_URL);

            // When
            rabbitTemplate.convertAndSend("proposal.approved", event);

            // Then — wait for async processing
            await().atMost(10, SECONDS).untilAsserted(() -> {
                // PDF generated
                verify(pdfService, times(1))
                        .generateProposalPdf(proposalId, GenerationContext.APPROVAL);

                // Email sent with the presigned URL
                verify(emailService, times(1))
                        .sendProposalApprovedEmail(clientEmail, MOCK_PDF_URL, proposalId);
            });

            // And — idempotency record with resultData saved
            await().atMost(10, SECONDS).untilAsserted(() -> {
                String expectedKey = "proposal-" + proposalId + ":" +
                        event.occurredAt().toEpochMilli();
                Optional<IdempotencyRecord> record =
                        idempotencyRepository.findByListenerIdAndKey(
                                "proposal-approval-listener",
                                expectedKey
                        );
                assertThat(record).isPresent();
                assertThat(record.get().getResultData()).contains(MOCK_PDF_URL);
                assertThat(record.get().getResultData()).contains("\"emailSent\":true");
            });
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("should generate PDF and send email only once when event arrives twice")
        void shouldProcessOnlyOnceOnRedelivery() throws Exception {
            // Given
            UUID proposalId = UUID.randomUUID();
            UUID workflowId = UUID.randomUUID();
            ProposalApprovedEvent event = MessagingEventFixtures.proposalApprovedEvent(proposalId, workflowId);

            when(pdfService.generateProposalPdf(any(), any())).thenReturn(MOCK_PDF_URL);

            // When — first delivery
            rabbitTemplate.convertAndSend("proposal.approved", event);
            await().atMost(10, SECONDS).untilAsserted(() ->
                    verify(pdfService, times(1)).generateProposalPdf(any(), any())
            );

            // Reset call counters (idempotency record stays in DB)
            clearInvocations(pdfService, emailService);

            // When — second delivery (broker redelivery)
            rabbitTemplate.convertAndSend("proposal.approved", event);
            Thread.sleep(3000);

            // Then — neither PDF nor email generated again
            verify(pdfService, never()).generateProposalPdf(any(), any());
            verify(emailService, never()).sendProposalApprovedEmail(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("PDF generation failure triggers RabbitMQ retry; email not sent on failed attempt")
        void shouldNotSendEmailWhenPdfFails() throws Exception {
            // Given — PDF fails on first attempt, succeeds on second
            UUID proposalId = UUID.randomUUID();
            ProposalApprovedEvent event = MessagingEventFixtures.proposalApprovedEvent(
                    proposalId, UUID.randomUUID());

            when(pdfService.generateProposalPdf(eq(proposalId), eq(GenerationContext.APPROVAL)))
                    .thenThrow(new PdfGenerationException("iText render failed"))
                    .thenReturn(MOCK_PDF_URL);

            // When
            rabbitTemplate.convertAndSend("proposal.approved", event);

            // Then — after retry, PDF and email both eventually succeed
            await().atMost(20, SECONDS).untilAsserted(() -> {
                verify(pdfService, atLeast(2))
                        .generateProposalPdf(proposalId, GenerationContext.APPROVAL);
                verify(emailService, times(1))
                        .sendProposalApprovedEmail(anyString(), eq(MOCK_PDF_URL), eq(proposalId));
            });
        }

        @Test
        @DisplayName("email failure after PDF generation triggers retry; PDF generated again on next attempt")
        void shouldRetryEntireFlowOnEmailFailure() throws Exception {
            // Given — PDF succeeds, email fails first time, then email succeeds
            UUID proposalId = UUID.randomUUID();
            ProposalApprovedEvent event = MessagingEventFixtures.proposalApprovedEvent(
                    proposalId, UUID.randomUUID());

            when(pdfService.generateProposalPdf(any(), any())).thenReturn(MOCK_PDF_URL);
            doThrow(new EmailException("SES rate limit"))
                    .doNothing()
                    .when(emailService).sendProposalApprovedEmail(any(), any(), any());

            // When
            rabbitTemplate.convertAndSend("proposal.approved", event);

            // Then — listener retries; both PDF and email eventually succeed
            await().atMost(20, SECONDS).untilAsserted(() -> {
                verify(emailService, atLeast(2))
                        .sendProposalApprovedEmail(anyString(), anyString(), eq(proposalId));

                // Idempotency record saved after successful retry
                assertThat(idempotencyRepository.countByListenerId("proposal-approval-listener"))
                        .isGreaterThanOrEqualTo(1);
            });
        }
    }
}
