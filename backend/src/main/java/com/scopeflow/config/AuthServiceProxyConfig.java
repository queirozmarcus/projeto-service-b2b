package com.scopeflow.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for proxying auth and user requests to user-service.
 *
 * RestTemplate configured with timeouts to prevent cascading failures.
 * Connect timeout: 5s. Read timeout: 30s (user-service cold start can take 20s+).
 */
@Configuration
public class AuthServiceProxyConfig {

    @Bean
    public RestTemplate authServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))  // Increased: user-service can take 20s+ after cold start
                .build();
    }
}
