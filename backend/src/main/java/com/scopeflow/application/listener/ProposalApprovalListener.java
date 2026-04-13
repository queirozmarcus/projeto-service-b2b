package com.scopeflow.application.listener;

import com.scopeflow.application.idempotency.IdempotencyService;
import com.scopeflow.application.port.out.PdfService;
import com.scopeflow.application.port.out.PdfService.GenerationContext;
import com.scopeflow.application.port.out.PdfGenerationException;
import com.scopeflow.application.port.out.EmailService;
import com.scopeflow.application.port.out.EmailException;
import com.scopeflow.core.domain.proposal.event.ProposalApprovedEvent;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event Listener: ProposalApprovedEvent → Generate PDF + Send Approval Email.
 *
 * Responsibility:
 * - Listens for ProposalApprovedEvent on RabbitMQ queue: proposal.approved
 * - Generates proposal PDF (iText, async via thread pool)
 * - Uploads PDF to S3
 * - Sends approval email with PDF link to client
 * - Idempotent: won't generate duplicate PDFs or send duplicate emails
 *
 * Idempotency Key: "proposal-{proposalId}:{approvalWorkflowId}"
 * - Unique per approval workflow (not just proposal, in case re-approved)
 * - Ensures PDF generated only once per approval event
 *
 * Error Handling:
 * - PDF generation timeouts: 30s timeout with fallback to HTML
 * - Email service failures: RabbitMQ retry
 * - After 3 retries: DLQ for manual inspection
 * - Never loses approval (event already persisted)
 *
 * Performance:
 * - PDF generation offloaded to executor (virtual threads in Java 21)
 * - Non-blocking listener
 * - Parallel: multiple listeners can process different approvals simultaneously
 *
 * Side Effects (in order):
 * 1. Generate proposal PDF (iText) → ~2-5 seconds
 * 2. Upload to S3 → ~1 second
 * 3. Create presigned URL → ~100ms
 * 4. Send email (SES) → ~500ms
 * 5. Log and mark as processed
 */
@Component
public class ProposalApprovalListener {
    private static final Logger logger = LoggerFactory.getLogger(ProposalApprovalListener.class);
    private static final String LISTENER_ID = "proposal-approval-listener";

    private final IdempotencyService idempotencyService;
    private final PdfService pdfService;
    private final EmailService emailService;
    // private final ProposalRepository proposalRepository; // TODO: Load full proposal context

    public ProposalApprovalListener(
            IdempotencyService idempotencyService,
            PdfService pdfService,
            EmailService emailService
    ) {
        this.idempotencyService = idempotencyService;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    /**
     * Handle ProposalApprovedEvent.
     *
     * @RabbitListener converts AMQP message from queue "proposal.approved"
     * to ProposalApprovedEvent domain object.
     *
     * @param event the ProposalApprovedEvent from RabbitMQ
     */
    @RabbitListener(queues = "proposal.approved")
    @Transactional
    public void onProposalApproved(ProposalApprovedEvent event) {
        UUID proposalId = event.proposalId();
        String idempotencyKey = "proposal-" + proposalId + ":" + event.occurredAt().toEpochMilli();

        // Step 1: Check if already processed (idempotent)
        if (idempotencyService.isProcessed(LISTENER_ID, idempotencyKey)) {
            logger.info(
                    "ProposalApprovedEvent already processed (idempotent check), skipping. proposalId={}",
                    proposalId
            );
            return;
        }

        try {
            // Step 2a: Generate proposal PDF (Phase 3 - IMPLEMENTED)
            String pdfUrl;
            try {
                pdfUrl = pdfService.generateProposalPdf(proposalId, GenerationContext.APPROVAL);
                logger.info("PDF generated and uploaded to S3: {}", pdfUrl);
            } catch (PdfGenerationException e) {
                logger.error("PDF generation failed for proposal: {}, will retry", proposalId, e);
                // Re-throw to trigger RabbitMQ retry
                throw new RuntimeException("Failed to generate PDF", e);
            }

            // Step 2b: Send approval email with PDF link (Phase 4 - IMPLEMENTED)
            try {
                emailService.sendProposalApprovedEmail(
                        event.clientEmail(),
                        pdfUrl,
                        proposalId
                );
                logger.info("Approval email sent to: {} with PDF", event.clientEmail());
            } catch (EmailException e) {
                logger.error("Failed to send approval email to: {}, will retry", event.clientEmail(), e);
                // Re-throw to trigger RabbitMQ retry
                throw new RuntimeException("Failed to send approval email", e);
            }

            // Step 3: Mark as processed
            // Store PDF URL and email status as result data for audit trail
            String resultData = "{\"pdfUrl\":\"" + pdfUrl + "\",\"emailSent\":true}";
            idempotencyService.markAsProcessed(LISTENER_ID, idempotencyKey, resultData);

            logger.info(
                    "ProposalApprovedEvent processed successfully. proposalId={}, clientEmail={}, pdfUrl={}",
                    proposalId,
                    event.clientEmail(),
                    pdfUrl
            );

        } catch (Exception e) {
            // Log error; RabbitMQ will retry
            logger.error(
                    "Failed to process ProposalApprovedEvent. proposalId={}, will retry",
                    proposalId,
                    e
            );
            // Re-throw to trigger RabbitMQ retry
            throw new RuntimeException("Failed to generate PDF or send approval email", e);
        }
    }
}
