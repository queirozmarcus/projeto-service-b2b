package com.scopeflow.adapter.in.web.briefing.fixtures;

import com.scopeflow.core.domain.briefing.ServiceType;

import java.util.UUID;

/**
 * Constants and test data for briefing tests.
 */
public class BriefingTestData {

    // Default values
    public static final ServiceType DEFAULT_SERVICE = ServiceType.SOCIAL_MEDIA;
    public static final String VALID_ANSWER = "This is a valid test answer with enough content";
    public static final String EMPTY_ANSWER = "";
    public static final String TOO_LONG_ANSWER = "a".repeat(5001); // Max 5000

    // Test UUIDs
    public static final UUID VALID_CLIENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID VALID_WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID NONEXISTENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    // Completion scores
    public static final int MIN_COMPLETION_SCORE = 80;
    public static final int MAX_COMPLETION_SCORE = 100;
    public static final int INCOMPLETE_SCORE = 70;

    // Rate limiting
    public static final int AUTH_RATE_LIMIT = 100; // requests per minute
    public static final int PUBLIC_RATE_LIMIT = 10; // requests per minute
}
