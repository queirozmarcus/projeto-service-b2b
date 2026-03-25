package com.scopeflow.application.fixtures;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scopeflow.application.idempotency.IdempotencyRepository;
import com.scopeflow.application.outbox.OutboxEventRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Sprint 4 messaging/event integration tests.
 *
 * Provides:
 * - Testcontainers PostgreSQL 16-Alpine (Flyway V1-V6 applied)
 * - Testcontainers RabbitMQ 3.13-Alpine (real broker — queues declared via RabbitMQConfig)
 * - Full Spring Boot context with @ActiveProfiles("test")
 * - Shared repositories for assertion and cleanup
 * - ObjectMapper for JSON serialization helpers
 *
 * Design: Both containers are static (singleton pattern) — started once per JVM.
 * This avoids the ~5s per-class startup penalty while keeping tests isolated via @AfterEach cleanup.
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Import(TestAwsConfig.class)
public abstract class MessagingIntegrationTestBase {

    // Singleton containers — shared across all subclasses in the same JVM
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("scopeflow_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static final RabbitMQContainer RABBITMQ =
            new RabbitMQContainer("rabbitmq:3.13-alpine");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", () -> RABBITMQ.getMappedPort(5672));
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    // ============ Common injected beans ============

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected IdempotencyRepository idempotencyRepository;

    @Autowired
    protected OutboxEventRepository outboxEventRepository;
}
