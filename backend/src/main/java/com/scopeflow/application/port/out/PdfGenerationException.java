package com.scopeflow.application.port.out;

/**
 * Checked exception for PDF generation failures.
 * Thrown by PdfService implementations when iText rendering or S3 upload fails.
 * Triggers RabbitMQ retry when re-thrown from a listener.
 */
public class PdfGenerationException extends Exception {

    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
