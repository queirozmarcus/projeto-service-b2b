package com.scopeflow.adapter.out.email;

import com.scopeflow.application.port.out.EmailService;
import com.scopeflow.application.port.out.EmailException;
import com.scopeflow.core.domain.user.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

/**
 * AWS SES Email Service Adapter.
 *
 * Sends transactional emails via AWS Simple Email Service (SES).
 *
 * Configuration:
 * - SMTP via JavaMailSender (Spring Mail auto-config with SES)
 * - Or direct SESv2 API calls (faster, lower latency)
 *
 * Sender Email:
 * - Default: noreply@scopeflow.com (must be verified in SES)
 * - Configurable via application.properties: mail.from
 *
 * Email Limits:
 * - SES Sandbox: 200 emails/day (development)
 * - Production: depends on quota increase request
 * - Rate: ~100 emails/sec max
 *
 * Error Handling:
 * - Invalid email: ValidationException (log, don't retry)
 * - SES quota exceeded: ThrottlingException (listener retries via RabbitMQ)
 * - Network failure: IOException (listener retries)
 * - Template error: TemplateRenderingException (log, fallback to plain text)
 *
 * Circuit Breaker "ses":
 * - Opens after 60% failure rate in sliding window of 5 calls
 * - Remains open for 30s before attempting half-open
 * - Half-open: 2 probe calls before closing
 * - Fallback: throws ServiceUnavailableException → GlobalExceptionHandler returns 503
 * - No @Retry: SES errors are usually quota/auth issues (not transient network), retried by RabbitMQ
 *
 * Performance:
 * - SES latency: ~500ms per email
 * - Async execution: virtual threads (non-blocking)
 * - Batch: SES supports batch send (not implemented yet)
 *
 * TODO (Future):
 * - Email logging to audit table (email_log)
 * - Bounce/complaint handling (SNS topic)
 * - Template versioning (match with domain events)
 * - A/B testing of subject lines
 */
@Component
public class AwsSesEmailServiceAdapter implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(AwsSesEmailServiceAdapter.class);

    @Value("${mail.from:noreply@scopeflow.com}")
    private String senderEmail;

    private final SesV2Client sesClient;
    private final JavaMailSender mailSender;

    public AwsSesEmailServiceAdapter(SesV2Client sesClient, JavaMailSender mailSender) {
        this.sesClient = sesClient;
        this.mailSender = mailSender;
    }

    @Override
    @CircuitBreaker(name = "ses", fallbackMethod = "sendWelcomeEmailFallback")
    public void sendWelcomeEmail(String email, String fullName, UUID workspaceId) throws EmailException {
        try {
            String subject = "Welcome to ScopeFlow!";
            String htmlBody = buildWelcomeHtml(fullName, workspaceId);
            String plainTextBody = "Welcome to ScopeFlow, " + fullName + "! " +
                    "Visit your workspace: https://app.scopeflow.com/workspace/" + workspaceId;

            sendEmailViaSES(email, subject, htmlBody, plainTextBody);

            logger.info("Welcome email sent to: {} (fullName: {}, workspaceId: {})", email, fullName, workspaceId);

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send welcome email to: {}", email, e);
            throw new EmailException("Failed to send welcome email", e);
        }
    }

    @Override
    @CircuitBreaker(name = "ses", fallbackMethod = "sendProposalApprovedEmailFallback")
    public void sendProposalApprovedEmail(String email, String pdfUrl, UUID proposalId) throws EmailException {
        try {
            String subject = "Your Proposal Has Been Approved!";
            String htmlBody = buildProposalApprovedHtml(pdfUrl, proposalId);
            String plainTextBody = "Your proposal (ID: " + proposalId + ") has been approved! " +
                    "Download your PDF: " + pdfUrl;

            sendEmailViaSES(email, subject, htmlBody, plainTextBody);

            logger.info("Proposal approved email sent to: {} (proposalId: {})", email, proposalId);

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send proposal approved email to: {}", email, e);
            throw new EmailException("Failed to send proposal approved email", e);
        }
    }

    @Override
    @CircuitBreaker(name = "ses", fallbackMethod = "sendBriefingCompletionEmailFallback")
    public void sendBriefingCompletionEmail(String email, UUID sessionId) throws EmailException {
        try {
            String subject = "Complete Your Briefing";
            String htmlBody = buildBriefingReminderHtml(sessionId);
            String plainTextBody = "Please complete your briefing session: " +
                    "https://app.scopeflow.com/briefing/" + sessionId;

            sendEmailViaSES(email, subject, htmlBody, plainTextBody);

            logger.info("Briefing completion email sent to: {} (sessionId: {})", email, sessionId);

        } catch (ServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Failed to send briefing completion email to: {}", email, e);
            throw new EmailException("Failed to send briefing completion email", e);
        }
    }

    // ============ Fallbacks (circuit breaker open) ============

    /**
     * Chamado quando o circuit breaker "ses" está aberto para sendWelcomeEmail.
     * Lança ServiceUnavailableException → GlobalExceptionHandler retorna 503.
     */
    @SuppressWarnings("unused")
    public void sendWelcomeEmailFallback(String email, String fullName, UUID workspaceId, Exception ex)
            throws EmailException {
        logger.warn("Circuit breaker open for ses (sendWelcomeEmail): recipient={}", email);
        throw new ServiceUnavailableException("ses", ex);
    }

    /**
     * Chamado quando o circuit breaker "ses" está aberto para sendProposalApprovedEmail.
     */
    @SuppressWarnings("unused")
    public void sendProposalApprovedEmailFallback(String email, String pdfUrl, UUID proposalId, Exception ex)
            throws EmailException {
        logger.warn("Circuit breaker open for ses (sendProposalApprovedEmail): recipient={}", email);
        throw new ServiceUnavailableException("ses", ex);
    }

    /**
     * Chamado quando o circuit breaker "ses" está aberto para sendBriefingCompletionEmail.
     */
    @SuppressWarnings("unused")
    public void sendBriefingCompletionEmailFallback(String email, UUID sessionId, Exception ex)
            throws EmailException {
        logger.warn("Circuit breaker open for ses (sendBriefingCompletionEmail): recipient={}", email);
        throw new ServiceUnavailableException("ses", ex);
    }

    /**
     * Send email via AWS SES (direct API).
     *
     * Uses SESv2 API for lower latency and better control.
     */
    private void sendEmailViaSES(String recipient, String subject, String htmlBody, String plainTextBody) {
        SendEmailRequest sendEmailRequest = SendEmailRequest.builder()
                .fromEmailAddress(senderEmail)
                .destination(Destination.builder()
                        .toAddresses(recipient)
                        .build())
                .content(EmailContent.builder()
                        .simple(software.amazon.awssdk.services.sesv2.model.Message.builder()
                                .subject(Content.builder().data(subject).build())
                                .body(Body.builder()
                                        .html(Content.builder().data(htmlBody).build())
                                        .text(Content.builder().data(plainTextBody).build())
                                        .build())
                                .build())
                        .build())
                .build();

        SendEmailResponse response = sesClient.sendEmail(sendEmailRequest);
        logger.debug("SES email sent. MessageId: {}, Recipient: {}", response.messageId(), recipient);
    }

    /**
     * Build welcome email HTML body.
     *
     * TODO: Load from Thymeleaf template (resources/templates/email/welcome.html)
     */
    private String buildWelcomeHtml(String fullName, UUID workspaceId) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h1>Welcome to ScopeFlow, " + escapeHtml(fullName) + "!</h1>" +
                "<p>We're excited to have you on board.</p>" +
                "<p><a href='https://app.scopeflow.com/workspace/" + workspaceId + "'>Go to your workspace</a></p>" +
                "<p>Questions? Email us at support@scopeflow.com</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build proposal approved email HTML body.
     *
     * TODO: Load from Thymeleaf template (resources/templates/email/proposal-approved.html)
     */
    private String buildProposalApprovedHtml(String pdfUrl, UUID proposalId) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h1>Your Proposal Has Been Approved!</h1>" +
                "<p>Congratulations! Your proposal (ID: " + proposalId + ") has been approved.</p>" +
                "<p><a href='" + escapeHtml(pdfUrl) + "'>Download Your PDF</a></p>" +
                "<p>Next steps: Your project team will be in touch shortly.</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Build briefing reminder email HTML body.
     *
     * TODO: Load from Thymeleaf template (resources/templates/email/briefing-reminder.html)
     */
    private String buildBriefingReminderHtml(UUID sessionId) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<h1>Complete Your Briefing</h1>" +
                "<p>We noticed you haven't finished your briefing yet.</p>" +
                "<p><a href='https://app.scopeflow.com/briefing/" + sessionId + "'>Complete Briefing</a></p>" +
                "<p>It only takes a few minutes!</p>" +
                "</body>" +
                "</html>";
    }

    /**
     * Escape HTML special characters for safe rendering.
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
