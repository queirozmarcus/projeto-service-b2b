package com.scopeflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * AWS Services Configuration.
 *
 * Configures AWS SDK v2 clients for:
 * - S3: PDF upload and presigned URLs
 * - SESv2: Email sending (Phase 4)
 *
 * Credentials: Loaded from environment variables or IAM role.
 * - AWS_ACCESS_KEY_ID
 * - AWS_SECRET_ACCESS_KEY
 * - AWS_REGION (default: us-east-1)
 *
 * For local development: Use LocalStack or mock credentials.
 * For production: Use IAM role (EC2, ECS, Lambda, etc.)
 */
@Configuration
public class AwsConfig {

    /**
     * S3Client bean for object operations.
     *
     * Used by PdfService to upload PDFs and other files.
     * Credentials loaded from environment or IAM role.
     *
     * @return configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder().build();
    }

    /**
     * S3Presigner bean for generating presigned URLs.
     *
     * Used by PdfService to generate temporary download links.
     * Presigned URLs are valid for configurable duration (10-30 days).
     *
     * @return configured S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder().build();
    }

    /**
     * SESv2Client bean for email sending (Phase 4).
     *
     * Used by EmailService to send emails via AWS SES.
     * Requires sender email registration in SES.
     *
     * @return configured SESv2Client
     */
    @Bean
    public SesV2Client sesClient() {
        return SesV2Client.builder().build();
    }
}
