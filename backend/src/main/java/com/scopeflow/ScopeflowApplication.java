package com.scopeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * ScopeFlow API Application.
 *
 * AI-powered briefing and scope alignment SaaS for B2B service providers.
 */
@SpringBootApplication
@EnableAsync
@EnableAspectJAutoProxy
public class ScopeflowApplication {

  public static void main(String[] args) {
    SpringApplication.run(ScopeflowApplication.class, args);
  }
}
