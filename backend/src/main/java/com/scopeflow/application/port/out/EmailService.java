package com.scopeflow.application.port.out;

import java.util.UUID;

/**
 * Output Port: Email Service.
 *
 * Responsibility: Send transactional emails via AWS SES.
 *
 * Supported Templates:
 * 1. Welcome Email (UserRegisteredEvent listener)
 *    - To: new user's email
 *    - Content: welcome message, account setup link
 *    - Retention: none (informational)
 *
 * 2. Proposal Approved Email (ProposalApprovedEvent listener)
 *    - To: client's email
 *    - Content: approval confirmation, PDF download link, next steps
 *    - Attachment: proposal PDF (via presigned URL)
 *    - Retention: audit trail in email_log table
 *
 * 3. Briefing Reminder (future)
 *    - To: client's email
 *    - Content: remind to complete briefing if incomplete
 *    - Retention: audit trail
 *
 * Error Handling:
 * - SES quota exceeded: throttle/retry (listener handles via RabbitMQ)
 * - Invalid email: log and continue (no retry)
 * - Network failure: throw exception → listener retries
 *
 * Audit Trail:
 * - All emails logged to email_log table (messageId, recipient, status, timestamp)
 * - Useful for: troubleshooting, compliance (LGPD), bounce handling
 *
 * Performance:
 * - SES send: ~500ms per email (network latency)
 * - Non-blocking: async execution
 * - Rate limiting: SES sandbox limit 200/day (configurable)
 */
public interface EmailService {

    /**
     * Send welcome email to new user.
     *
     * Called by: UserRegistrationListener (after successful registration)
     * Template: welcome.html (Thymeleaf)
     * Variables: email, fullName, workspaceId, loginUrl
     *
     * @param email the recipient's email address
     * @param fullName the user's full name
     * @param workspaceId the workspace they're joining
     * @throws EmailException if SES send fails
     */
    void sendWelcomeEmail(String email, String fullName, UUID workspaceId) throws EmailException;

    /**
     * Send proposal approved notification email.
     *
     * Called by: ProposalApprovalListener (after PDF generation + approval)
     * Template: proposal-approved.html (Thymeleaf)
     * Variables: email, clientName, proposalId, pdfUrl, approvalSignature
     *
     * @param email the client's email address
     * @param pdfUrl presigned S3 URL to the generated proposal PDF
     * @param proposalId the proposal ID (for reference)
     * @throws EmailException if SES send fails
     */
    void sendProposalApprovedEmail(String email, String pdfUrl, UUID proposalId) throws EmailException;

    /**
     * Send briefing completion reminder email (future).
     *
     * Called by: manual trigger or scheduled job
     * Template: briefing-reminder.html (Thymeleaf)
     * Variables: email, sessionId, briefingUrl, daysRemaining
     *
     * @param email the client's email address
     * @param sessionId the briefing session ID
     * @throws EmailException if SES send fails
     */
    void sendBriefingCompletionEmail(String email, UUID sessionId) throws EmailException;
}
