package com.scopeflow.adapter.in.web.briefing.integration;

import com.scopeflow.adapter.in.web.briefing.dto.CreateBriefingRequest;
import com.scopeflow.core.domain.briefing.ServiceType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for rate limiting.
 *
 * Validates rate limit enforcement:
 * - Authenticated endpoints: 100 req/min per user
 * - Public endpoints: 10 req/min per IP
 * - X-Rate-Limit-Remaining header
 * - Counter reset after window
 * - Per-user/per-IP isolation
 *
 * Coverage: Rate limit happy path + limit exceeded + reset behavior.
 */
class BriefingControllerRateLimitTest extends BriefingIntegrationTestBase {

    @Test
    void testAuthEndpoint_RateLimited100PerMin() throws Exception {
        // Given: authenticated user
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var request = new CreateBriefingRequest(UUID.randomUUID(), ServiceType.SOCIAL_MEDIA);

        // When: send 100 requests (should succeed)
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/api/v1/briefings")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // 101st request should be rate limited
        mockMvc.perform(post("/api/v1/briefings")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE-429"));
    }

    @Test
    void testPublicEndpoint_RateLimited10PerMin() throws Exception {
        // Given: briefing with valid public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: send 10 requests (should succeed)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                    .andExpect(status().isOk());
        }

        // 11th request should be rate limited
        mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("RATE-429"));
    }

    @Test
    void testRateLimitHeader_AuthEndpoint() throws Exception {
        // Given: authenticated user
        var token = generateTestJwtToken(WORKSPACE_ID_A, "user@example.com");
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        // When: make first request
        MvcResult result = mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Rate-Limit-Remaining"))
                .andReturn();

        // Then: verify header value
        String rateLimitHeader = result.getResponse().getHeader("X-Rate-Limit-Remaining");
        assertThat(rateLimitHeader).isNotNull();
        int remaining = Integer.parseInt(rateLimitHeader);
        assertThat(remaining).isLessThanOrEqualTo(99); // 100 - 1 request
        assertThat(remaining).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testRateLimitHeader_PublicEndpoint() throws Exception {
        // Given: briefing with valid public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: make first request
        MvcResult result = mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Rate-Limit-Remaining"))
                .andReturn();

        // Then: verify header value
        String rateLimitHeader = result.getResponse().getHeader("X-Rate-Limit-Remaining");
        assertThat(rateLimitHeader).isNotNull();
        int remaining = Integer.parseInt(rateLimitHeader);
        assertThat(remaining).isLessThanOrEqualTo(9); // 10 - 1 request
        assertThat(remaining).isGreaterThanOrEqualTo(0);
    }

    @Test
    void testRateLimitReset_AfterWindow() throws Exception {
        // Given: briefing and public token
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var publicToken = session.getPublicToken().value();

        // When: exhaust rate limit
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                    .andExpect(status().isOk());
        }

        // Verify rate limited
        mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isTooManyRequests());

        // Wait for window to reset (60 seconds)
        TimeUnit.SECONDS.sleep(61);

        // Then: should be able to make requests again
        mockMvc.perform(get("/public/briefings/{publicToken}", publicToken))
                .andExpect(status().isOk());
    }

    @Test
    void testRateLimitPerIP_Public() throws Exception {
        // Given: two different sessions (simulating different IPs)
        var session1 = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);
        var session2 = createTestBriefing(WORKSPACE_ID_A, UUID.randomUUID(), ServiceType.LANDING_PAGE);
        var token1 = session1.getPublicToken().value();
        var token2 = session2.getPublicToken().value();

        // When: exhaust rate limit for token1
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/public/briefings/{publicToken}", token1))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/public/briefings/{publicToken}", token1))
                .andExpect(status().isTooManyRequests());

        // Then: token2 should still have separate counter (in real scenario, different IP)
        // Note: In this test, both share same IP, so this may fail if rate limiting is by IP only
        // This test validates the concept; in production, separate IPs would have separate counters
        mockMvc.perform(get("/public/briefings/{publicToken}", token2)
                        .header("X-Forwarded-For", "192.168.1.2")) // Simulate different IP
                .andExpect(status().isOk());
    }

    @Test
    void testRateLimitPerUser_Auth() throws Exception {
        // Given: two different users
        var token1 = generateTestJwtToken(WORKSPACE_ID_A, "user1@example.com");
        var token2 = generateTestJwtToken(WORKSPACE_ID_A, "user2@example.com");
        var session = createTestBriefing(WORKSPACE_ID_A, CLIENT_ID, ServiceType.SOCIAL_MEDIA);

        // When: user1 exhausts rate limit
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                            .header("Authorization", token1))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                        .header("Authorization", token1))
                .andExpect(status().isTooManyRequests());

        // Then: user2 should have separate counter
        mockMvc.perform(get("/api/v1/briefings/{id}", session.getId().value())
                        .header("Authorization", token2))
                .andExpect(status().isOk());
    }
}
