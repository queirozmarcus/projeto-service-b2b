package com.scopeflow.adapter.out.pdf;

import com.scopeflow.application.port.out.PdfService;
import com.scopeflow.application.port.out.PdfGenerationException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * iText 8 PDF Generation Adapter (Phase 4 - Stub for Sprint 5).
 *
 * Placeholder implementation. Full implementation requires iText 8.x licensing.
 * Scheduled for Phase 4 (after Sprint 5 authentication is stable).
 *
 * For now: returns mock presigned URLs for testing approval flow.
 */
@Component
public class ITextPdfServiceAdapter implements PdfService {
    private static final Logger logger = LoggerFactory.getLogger(ITextPdfServiceAdapter.class);

    @Override
    public String generateProposalPdf(UUID proposalId, GenerationContext context) throws PdfGenerationException {
        logger.warn("PDF generation stubbed for Sprint 5. Phase 4 will implement full iText integration.");
        // Return mock presigned URL for testing
        return "https://s3.mock/proposals/" + proposalId + "/mock-presigned-url?expires=2026-04-24";
    }

    @Override
    public String generateKickoffPdf(UUID approvalWorkflowId) throws PdfGenerationException {
        logger.warn("PDF generation stubbed for Sprint 5. Phase 4 will implement full iText integration.");
        // Return mock presigned URL for testing
        return "https://s3.mock/kickoff/" + approvalWorkflowId + "/mock-presigned-url?expires=2026-04-24";
    }
}
