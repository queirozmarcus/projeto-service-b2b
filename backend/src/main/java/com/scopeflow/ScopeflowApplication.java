package com.scopeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ScopeFlow API Application.
 *
 * AI-powered briefing and scope alignment SaaS for B2B service providers.
 *
 * Features:
 * - @EnableAsync: for async method execution (virtual threads in Java 21)
 * - @EnableScheduling: for scheduled tasks (Outbox event poller, etc.)
 * - @EnableAspectJAutoProxy: for aspect-oriented programming
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableAspectJAutoProxy
public class ScopeflowApplication {

  public static void main(String[] args) {
    SpringApplication.run(ScopeflowApplication.class, args);
  }
}
