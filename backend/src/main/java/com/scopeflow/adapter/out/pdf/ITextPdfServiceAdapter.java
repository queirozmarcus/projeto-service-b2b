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
 *
 * TODO (Phase 4 — S3 integration):
 * When this adapter starts uploading PDFs to S3 via AWS SDK, add:
 * - @CircuitBreaker(name = "s3", fallbackMethod = "{method}Fallback")
 * - Fallback throwing ServiceUnavailableException("s3", ex)
 * - resilience4j.circuitbreaker.instances.s3 config in application.yml
 *   (suggested: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=30s)
 *
 * TODO (Phase 4 — OpenAI integration):
 * When OpenAIAssistantAdapter is implemented (AIAssistantPort), add:
 * - @CircuitBreaker(name = "openai", fallbackMethod = "{method}Fallback")
 * - @Retry(name = "openai") for transient network errors
 * - Fallback throwing ServiceUnavailableException("openai", ex)
 * - resilience4j.circuitbreaker.instances.openai config in application.yml
 *   (suggested: slidingWindowSize=10, failureRateThreshold=50, waitDurationInOpenState=60s — AI has higher latency)
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
