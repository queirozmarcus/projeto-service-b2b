package com.scopeflow.application.port.out;

import java.util.UUID;

/**
 * Output Port: PDF Generation Service.
 *
 * Responsibility: Generate proposal PDFs in various contexts.
 * Implementation: iText 8 + S3 upload + presigned URLs.
 *
 * Lifecycle:
 * 1. ProposalApprovalListener calls generateProposalPdf()
 * 2. Service renders HTML → iText PDF
 * 3. PDF uploaded to S3: /proposals/{proposalId}/{versionId}.pdf
 * 4. Presigned URL generated (10-day validity)
 * 5. URL returned for email, client download, etc.
 *
 * Error Handling:
 * - Template not found: IOException (logs, throws)
 * - PDF generation timeout: 30s timeout, fallback to HTML
 * - S3 upload failure: PdfGenerationException (retry via listener)
 * - InvalidProposalException: proposal not found or not in valid state
 *
 * Performance:
 * - Runs in virtual thread pool (Java 21)
 * - Async: ~2-5 seconds per PDF (iText rendering)
 * - S3 upload: ~1 second
 */
public interface PdfService {

    /**
     * Generate proposal PDF in approval context.
     *
     * Context determines what data is included in the PDF:
     * - APPROVAL: Full scope, deliverables, exclusions, timeline, price
     * - KICKOFF: After approval, includes approval signature, next steps
     * - PREVIEW: Draft preview for client review before approval
     *
     * @param proposalId UUID of the proposal to generate PDF for
     * @param context the generation context (determines template and data)
     * @return presigned S3 URL with 10-day validity
     * @throws PdfGenerationException if PDF generation or S3 upload fails
     * @throws InvalidProposalStateException if proposal is not in valid state
     * @throws com.scopeflow.core.domain.proposal.ProposalNotFoundException if proposal not found
     */
    String generateProposalPdf(UUID proposalId, GenerationContext context) throws PdfGenerationException;

    /**
     * Generate kickoff summary PDF (after proposal approval).
     *
     * Includes:
     * - Approval signature (name, date, IP)
     * - Project summary
     * - Deliverables and timeline
     * - Next steps and contact info
     *
     * @param approvalWorkflowId UUID of the approval workflow
     * @return presigned S3 URL with 30-day validity
     * @throws PdfGenerationException if generation or upload fails
     */
    String generateKickoffPdf(UUID approvalWorkflowId) throws PdfGenerationException;

    /**
     * Generation context determines PDF template and data inclusion.
     */
    enum GenerationContext {
        APPROVAL,  // Full proposal for client approval
        KICKOFF,   // Post-approval project kickoff summary
        PREVIEW    // Draft preview (not yet approved)
    }
}

/**
 * Checked exception for PDF generation failures.
 */
class PdfGenerationException extends Exception {
    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
