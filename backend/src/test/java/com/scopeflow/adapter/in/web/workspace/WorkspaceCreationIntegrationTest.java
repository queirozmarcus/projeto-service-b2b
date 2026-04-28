package com.scopeflow.adapter.in.web.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.adapter.in.web.workspace.dto.CreateWorkspaceRequest;
import com.scopeflow.application.outbox.OutboxEventRepository;
import com.scopeflow.config.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração para WorkspaceControllerV2.create().
 *
 * Estratégia:
 * - Testcontainers PostgreSQL para banco real com Flyway
 * - MockRestServiceServer para interceptar chamadas HTTP ao user-service
 *   (sem WireMock — padrão do projeto usa RestTemplate + MockRestServiceServer)
 * - JWT real via JwtService (mesmo padrão de BriefingIntegrationTestBase)
 *
 * Casos testados:
 * 1. Happy path: 201, user-service recebe PATCH → 204, sem header X-Workspace-Pending
 * 2. user-service indisponível (503): 201 + X-Workspace-Pending: true + outbox entry criado
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WorkspaceCreationIntegrationTest {

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RestTemplate authServiceRestTemplate;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private MockRestServiceServer mockUserService;

    private static final UUID OWNER_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    // workspace_id null no token: owner ainda não tem workspace (fluxo de criação)
    private static final UUID TOKEN_WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        mockUserService = MockRestServiceServer.createServer(authServiceRestTemplate);
    }

    @AfterEach
    void tearDown() {
        outboxEventRepository.deleteAll();
        mockUserService.reset();
    }

    // ============ Happy path ============

    @Test
    void shouldReturn201_whenWorkspaceCreatedAndUserServiceNotified() throws Exception {
        // Given
        String bearerToken = generateToken(OWNER_ID, TOKEN_WORKSPACE_ID);

        mockUserService
                .expect(method(HttpMethod.PATCH))
                .andExpect(requestToUriTemplate(authServiceUrl + "/users/{userId}/workspace", OWNER_ID))
                .andRespond(withNoContent());

        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "Agência Test", "Marketing Digital", "Tom profissional e direto"
        );

        // When / Then
        mockMvc.perform(post("/workspaces")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("X-Workspace-Pending"))
                .andExpect(jsonPath("$.name").value("Agência Test"));

        mockUserService.verify();

        // Nenhum evento de outbox deve ter sido criado
        long pendingCount = outboxEventRepository.findUnpublishedByEventType("WORKSPACE_OWNER_ASSIGNED").size();
        assertThat(pendingCount).isZero();
    }

    // ============ user-service indisponível ============

    @Test
    void shouldReturn201WithPendingHeader_whenUserServiceUnavailable() throws Exception {
        // Given
        String bearerToken = generateToken(OWNER_ID, TOKEN_WORKSPACE_ID);

        mockUserService
                .expect(method(HttpMethod.PATCH))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
                "Agência Fallback", "Tech", null
        );

        // When / Then
        mockMvc.perform(post("/workspaces")
                        .header("Authorization", bearerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Workspace-Pending", "true"))
                .andExpect(jsonPath("$.name").value("Agência Fallback"));

        mockUserService.verify();

        // Outbox deve conter exatamente 1 evento WORKSPACE_OWNER_ASSIGNED
        var pending = outboxEventRepository.findUnpublishedByEventType("WORKSPACE_OWNER_ASSIGNED");
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getAggregateType()).isEqualTo("workspace");
        assertThat(pending.get(0).getPayload()).contains(OWNER_ID.toString());
    }

    // ============ Private helpers ============

    /**
     * Gera JWT real com os claims necessários (sub, email, workspace_id, role).
     * Mesmo padrão de generateTestJwtToken() em BriefingIntegrationTestBase.
     */
    private String generateToken(UUID userId, UUID workspaceId) {
        String token = jwtService.generateAccessToken(userId, "owner@test.com", workspaceId, "OWNER");
        return "Bearer " + token;
    }
}
