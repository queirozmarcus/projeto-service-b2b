package com.scopeflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.access-key-id:local-dev-key}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:local-dev-secret}")
    private String secretAccessKey;

    private StaticCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
        );
    }

    /**
     * S3Client bean for object operations.
     *
     * Used by PdfService to upload PDFs and other files.
     * Credentials loaded from environment or application.yml defaults.
     * For local/staging: uses static credentials to avoid AWS metadata service calls.
     *
     * @return configured S3Client
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider())
                .build();
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
        return S3Presigner.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * SESv2Client bean for email sending (Phase 4).
     *
     * Used by EmailService to send emails via AWS SES.
     * For local/staging: uses static credentials to avoid AWS metadata service calls.
     * Email sending is stubbed in non-production environments.
     *
     * @return configured SESv2Client
     */
    @Bean
    public SesV2Client sesClient() {
        return SesV2Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}
