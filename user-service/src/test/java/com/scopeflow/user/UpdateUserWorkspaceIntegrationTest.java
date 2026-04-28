package com.scopeflow.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.user.adapter.in.web.user.dto.UpdateUserWorkspaceRequest;
import com.scopeflow.user.adapter.out.persistence.JpaUser;
import com.scopeflow.user.adapter.out.persistence.JpaUserSpringRepository;
import com.scopeflow.user.application.usecase.UpdateUserWorkspaceUseCase;
import com.scopeflow.user.config.JwtService;
import com.scopeflow.user.domain.model.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test: workspace assignment flow — from use case to repository.
 *
 * Tests persist through Testcontainers PostgreSQL, validating that workspaceId
 * is correctly stored and read back via JpaUserSpringRepository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("UpdateUserWorkspace — Integration Tests")
class UpdateUserWorkspaceIntegrationTest {

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
    }

    private MockMvc mockMvc;

    @Autowired private WebApplicationContext webApplicationContext;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JpaUserSpringRepository userRepository;
    @Autowired private UpdateUserWorkspaceUseCase updateUserWorkspaceUseCase;
    @Autowired private JwtService jwtService;

    private static final String BCRYPT_HASH = "$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
    private static final String INTERNAL_TOKEN = "dev-internal-token-change-in-prod";

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        userRepository.deleteAll();
    }

    // ============ Use case + repository level ============

    @Test
    @DisplayName("should persist workspaceId after execute when user is active and has no workspace")
    void shouldPersistWorkspaceId_whenUserActiveAndNoWorkspace() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        JpaUser saved = userRepository.save(activeUserWithoutWorkspace(userId));

        // When
        updateUserWorkspaceUseCase.execute(new UserId(saved.getId()), workspaceId);

        // Then
        Optional<JpaUser> updated = userRepository.findById(saved.getId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getWorkspaceId()).isEqualTo(workspaceId);
    }

    @Test
    @DisplayName("should be idempotent when same workspaceId is provided twice")
    void shouldBeIdempotent_whenSameWorkspaceIdAssignedTwice() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        userRepository.save(activeUserWithoutWorkspace(userId));

        // When — first call persists
        updateUserWorkspaceUseCase.execute(new UserId(userId), workspaceId);
        // When — second call with same workspaceId is a no-op (idempotent)
        updateUserWorkspaceUseCase.execute(new UserId(userId), workspaceId);

        // Then — only one workspace assignment persisted
        Optional<JpaUser> updated = userRepository.findById(userId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getWorkspaceId()).isEqualTo(workspaceId);
    }

    // ============ HTTP level via internal token ============

    @Test
    @DisplayName("should return 204 via HTTP when internal token authorizes the call")
    void shouldReturn204_viaHttp_whenInternalTokenUsed() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        userRepository.save(activeUserWithoutWorkspace(userId));

        UpdateUserWorkspaceRequest request = new UpdateUserWorkspaceRequest(workspaceId);

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Optional<JpaUser> updated = userRepository.findById(userId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getWorkspaceId()).isEqualTo(workspaceId);
    }

    @Test
    @DisplayName("should return 204 via HTTP when JWT owner calls with own userId")
    void shouldReturn204_viaHttp_whenJwtOwnerCallsWithOwnUserId() throws Exception {
        // Given
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        JpaUser user = userRepository.save(activeUserWithoutWorkspace(userId));
        String token = jwtService.generateAccessToken(user.getId(), user.getEmail(), null, "OWNER");

        UpdateUserWorkspaceRequest request = new UpdateUserWorkspaceRequest(workspaceId);

        // When / Then
        mockMvc.perform(patch("/api/v1/users/{userId}/workspace", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        Optional<JpaUser> updated = userRepository.findById(userId);
        assertThat(updated).isPresent();
        assertThat(updated.get().getWorkspaceId()).isEqualTo(workspaceId);
    }

    // ============ Helpers ============

    private JpaUser activeUserWithoutWorkspace(UUID id) {
        return new JpaUser(id, "user-" + id + "@example.com", BCRYPT_HASH,
                "Test User", null, "ACTIVE", Instant.now(), Instant.now());
    }
}
