package com.scopeflow.adapter.in.web.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSession;
import com.scopeflow.adapter.out.persistence.briefing.JpaBriefingSessionSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaApprovalSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaApprovalWorkflowSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalSpringRepository;
import com.scopeflow.adapter.out.persistence.proposal.JpaProposalVersionSpringRepository;
import com.scopeflow.adapter.out.persistence.user.JpaUser;
import com.scopeflow.adapter.out.persistence.user.JpaUserSpringRepository;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspace;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMember;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceMemberSpringRepository;
import com.scopeflow.adapter.out.persistence.workspace.JpaWorkspaceSpringRepository;
import com.scopeflow.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for Sprint 2 integration tests (auth, workspace, proposal).
 *
 * Provides:
 * - Testcontainers PostgreSQL 16-Alpine (shared per test class via @Container static)
 * - Full Spring Boot context with all Flyway migrations applied (V1-V4)
 * - MockMvc for HTTP requests against all registered controllers
 * - JwtService for generating real signed tokens in tests
 * - JPA Spring repositories for test data setup and assertion
 * - cleanDatabase() per test to ensure isolation
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
public abstract class ScopeFlowIntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scopeflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        // Point RabbitMQ to localhost (connection will fail gracefully — no AMQP messaging in tests)
        registry.add("spring.rabbitmq.host", () -> "localhost");
    }

    // ============ Injected Beans ============

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtService jwtService;

    // Repositories for test data setup
    @Autowired
    protected JpaUserSpringRepository userRepository;

    @Autowired
    protected JpaWorkspaceSpringRepository workspaceRepository;

    @Autowired
    protected JpaWorkspaceMemberSpringRepository memberRepository;

    @Autowired
    protected JpaProposalSpringRepository proposalRepository;

    @Autowired
    protected JpaProposalVersionSpringRepository proposalVersionRepository;

    @Autowired
    protected JpaBriefingSessionSpringRepository briefingSessionRepository;

    @Autowired
    protected JpaApprovalWorkflowSpringRepository workflowRepository;

    @Autowired
    protected JpaApprovalSpringRepository approvalRepository;

    // ============ Test Data IDs ============

    protected static final String TEST_USER_EMAIL = "testuser@scopeflow.com";
    // BCrypt hash for "Password1!" — pre-computed, strength 12
    protected static final String TEST_PASSWORD_HASH =
            "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    protected static final String TEST_WORKSPACE_NAME = "Test Agency";
    protected static final String TEST_NICHE = "social-media";

    // ============ Lifecycle ============

    @BeforeEach
    void cleanDatabase() {
        // Delete in FK order to avoid constraint violations
        approvalRepository.deleteAll();
        workflowRepository.deleteAll();
        proposalVersionRepository.deleteAll();
        proposalRepository.deleteAll();
        briefingSessionRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ============ Helpers ============

    /**
     * Create and persist a test user in ACTIVE state.
     */
    protected JpaUser createActiveUser() {
        return createActiveUser(UUID.randomUUID(), TEST_USER_EMAIL);
    }

    protected JpaUser createActiveUser(UUID id, String email) {
        JpaUser user = new JpaUser(
                id,
                email,
                TEST_PASSWORD_HASH,
                "Test User",
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        return userRepository.save(user);
    }

    /**
     * Create and persist a test workspace.
     */
    protected JpaWorkspace createWorkspace(UUID ownerId, String name) {
        JpaWorkspace workspace = new JpaWorkspace(
                UUID.randomUUID(),
                ownerId,
                name,
                TEST_NICHE,
                null,
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        return workspaceRepository.save(workspace);
    }

    /**
     * Create and persist an OWNER workspace member.
     */
    protected JpaWorkspaceMember addOwnerMember(UUID workspaceId, UUID userId) {
        JpaWorkspaceMember member = new JpaWorkspaceMember(
                UUID.randomUUID(),
                workspaceId,
                userId,
                "OWNER",
                "ACTIVE",
                Instant.now(),
                Instant.now()
        );
        return memberRepository.save(member);
    }

    /**
     * Generate a signed JWT access token for a user in a workspace.
     */
    protected String bearerToken(UUID userId, String email, UUID workspaceId, String role) {
        String token = jwtService.generateAccessToken(userId, email, workspaceId, role);
        return "Bearer " + token;
    }

    /**
     * Create and persist a briefing session in COMPLETED state.
     * Required to satisfy the FK constraint when creating proposals.
     */
    protected JpaBriefingSession createBriefingSession(UUID workspaceId) {
        JpaBriefingSession session = new JpaBriefingSession(
                UUID.randomUUID(),
                workspaceId,
                UUID.randomUUID(),
                "SOCIAL_MEDIA",
                "COMPLETED",
                UUID.randomUUID().toString(), // public_token
                95,
                null,
                null,
                Instant.now(),
                Instant.now()
        );
        return briefingSessionRepository.save(session);
    }

    /**
     * Create and persist a DRAFT proposal linked to a workspace and briefing session.
     */
    protected com.scopeflow.adapter.out.persistence.proposal.JpaProposal createDraftProposal(
            UUID workspaceId,
            UUID briefingId
    ) {
        com.scopeflow.adapter.out.persistence.proposal.JpaProposal proposal =
                new com.scopeflow.adapter.out.persistence.proposal.JpaProposal(
                        UUID.randomUUID(),
                        workspaceId,
                        UUID.randomUUID(),
                        briefingId,
                        "Test Proposal",
                        "DRAFT",
                        null,
                        Instant.now(),
                        Instant.now()
                );
        return proposalRepository.save(proposal);
    }

    /**
     * Set up a full user+workspace and return a valid Authorization header value.
     * Convenience for tests that only need an authenticated context.
     */
    protected AuthContext setupAuthenticatedUser() {
        JpaUser user = createActiveUser();
        JpaWorkspace workspace = createWorkspace(user.getId(), TEST_WORKSPACE_NAME);
        addOwnerMember(workspace.getId(), user.getId());
        String token = bearerToken(user.getId(), user.getEmail(), workspace.getId(), "OWNER");
        return new AuthContext(user.getId(), workspace.getId(), token);
    }

    /**
     * Carries the IDs and token for a fully set up test user.
     */
    protected record AuthContext(UUID userId, UUID workspaceId, String authorizationHeader) {}
}
