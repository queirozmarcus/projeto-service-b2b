package com.scopeflow.application.fixtures;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sesv2.SesV2Client;

import java.net.URI;

/**
 * Test-only AWS configuration.
 *
 * Replaces AwsConfig beans with stubs that do not connect to real AWS.
 * Uses static fake credentials + dummy endpoint so that the Spring context
 * can start without AWS environment variables.
 *
 * PdfService and EmailService are @MockBean in tests that use async listeners,
 * so these AWS clients are never actually invoked — they just need to exist
 * as beans so that the adapter classes (AwsSesEmailServiceAdapter, ITextPdfServiceAdapter)
 * can be wired (if not mocked themselves) without errors.
 *
 * Usage: @Import(TestAwsConfig.class) on test base or individual test class.
 */
@TestConfiguration
public class TestAwsConfig {

    private static final StaticCredentialsProvider FAKE_CREDS =
            StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key")
            );

    /**
     * S3Client pointing to localhost:9999 (unused in tests — PdfService is mocked).
     */
    @Bean
    @Primary
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(FAKE_CREDS)
                .endpointOverride(URI.create("http://localhost:9999"))
                .build();
    }

    /**
     * S3Presigner pointing to localhost:9999 (unused in tests — PdfService is mocked).
     */
    @Bean
    @Primary
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(FAKE_CREDS)
                .endpointOverride(URI.create("http://localhost:9999"))
                .build();
    }

    /**
     * SesV2Client pointing to localhost:9999 (unused in tests — EmailService is mocked).
     */
    @Bean
    @Primary
    public SesV2Client sesClient() {
        return SesV2Client.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(FAKE_CREDS)
                .endpointOverride(URI.create("http://localhost:9999"))
                .build();
    }
}
