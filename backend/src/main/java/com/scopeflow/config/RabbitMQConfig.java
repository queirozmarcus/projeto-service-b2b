package com.scopeflow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ Configuration (D7 — Message Queue Strategy).
 *
 * Defines the messaging topology for ScopeFlow async events:
 * - 1 Topic Exchange (scopeflow.events)
 * - 3+ Queues with durable, exclusive, auto-delete settings
 * - Bindings via routing keys
 *
 * Topology:
 * ```
 * Domain Event (ApplicationEventPublisher)
 *   ↓ (OutboxEventPublisher publishes to RabbitMQ)
 * Exchange: scopeflow.events (topic)
 *   ├─ Queue: user.registered (routing: user.registered.*)
 *   ├─ Queue: proposal.approved (routing: proposal.approved.*)
 *   ├─ Queue: briefing.completed (routing: briefing.completed.*)
 *   └─ DLQ variants for failed messages
 * ```
 *
 * Listeners subscribe via @RabbitListener:
 * - UserRegistrationListener (welcome email)
 * - ProposalApprovalListener (PDF + approval email)
 * - BriefingCompletedListener (fallback question generation)
 *
 * Error Handling:
 * - Max 3 retries with exponential backoff (1s, 2s, 4s)
 * - After 3 failures, message goes to DLQ
 * - DLQ messages can be manually replayed or analyzed
 *
 * Durable Settings:
 * - durable: true — queues survive broker restart
 * - exclusive: false — multiple consumers can bind
 * - auto-delete: false — queues not auto-deleted
 * - autoAck: false (configured in application.yml) — manual acknowledgment ensures idempotency
 */
@Configuration
@EnableRabbit
public class RabbitMQConfig {

    // ============ Exchange ============

    /**
     * Main topic exchange for domain events.
     * Pattern: {domain}.{entity}.{action}.*
     */
    @Bean
    public Exchange scopeflowEventsExchange() {
        return new TopicExchange(
                "scopeflow.events",
                true,      // durable
                false      // auto-delete
        );
    }

    // ============ Queues ============

    /**
     * Queue for UserRegisteredEvent.
     * Listener: UserRegistrationListener → sends welcome email
     */
    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(
                "user.registered",
                true,      // durable
                false,     // exclusive
                false      // auto-delete
        );
    }

    /**
     * Dead Letter Queue for user.registered failures (3 retries exceeded).
     */
    @Bean
    public Queue userRegisteredDlq() {
        return new Queue(
                "user.registered.dlq",
                true,
                false,
                false
        );
    }

    /**
     * Queue for ProposalApprovedEvent.
     * Listener: ProposalApprovalListener → generates PDF + sends approval email
     */
    @Bean
    public Queue proposalApprovedQueue() {
        return new Queue(
                "proposal.approved",
                true,
                false,
                false
        );
    }

    /**
     * Dead Letter Queue for proposal.approved failures.
     */
    @Bean
    public Queue proposalApprovedDlq() {
        return new Queue(
                "proposal.approved.dlq",
                true,
                false,
                false
        );
    }

    /**
     * Queue for BriefingCompletedEvent.
     * Listener: BriefingCompletedListener → fallback question generation (if needed)
     */
    @Bean
    public Queue briefingCompletedQueue() {
        return new Queue(
                "briefing.completed",
                true,
                false,
                false
        );
    }

    /**
     * Dead Letter Queue for briefing.completed failures.
     */
    @Bean
    public Queue briefingCompletedDlq() {
        return new Queue(
                "briefing.completed.dlq",
                true,
                false,
                false
        );
    }

    // ============ Bindings ============

    /**
     * Bind user.registered queue to exchange with routing key.
     */
    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(userRegisteredQueue)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("user.registered.*");
    }

    /**
     * Bind user.registered.dlq (optional, for DLQ monitoring).
     */
    @Bean
    public Binding userRegisteredDlqBinding(Queue userRegisteredDlq, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(userRegisteredDlq)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("user.registered.dlq.*");
    }

    /**
     * Bind proposal.approved queue to exchange.
     */
    @Bean
    public Binding proposalApprovedBinding(Queue proposalApprovedQueue, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(proposalApprovedQueue)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("proposal.approved.*");
    }

    /**
     * Bind proposal.approved.dlq.
     */
    @Bean
    public Binding proposalApprovedDlqBinding(Queue proposalApprovedDlq, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(proposalApprovedDlq)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("proposal.approved.dlq.*");
    }

    /**
     * Bind briefing.completed queue to exchange.
     */
    @Bean
    public Binding briefingCompletedBinding(Queue briefingCompletedQueue, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(briefingCompletedQueue)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("briefing.completed.*");
    }

    /**
     * Bind briefing.completed.dlq.
     */
    @Bean
    public Binding briefingCompletedDlqBinding(Queue briefingCompletedDlq, Exchange scopeflowEventsExchange) {
        return BindingBuilder
                .bind(briefingCompletedDlq)
                .to((TopicExchange) scopeflowEventsExchange)
                .with("briefing.completed.dlq.*");
    }
}
