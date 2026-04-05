package com.scopeflow.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for proxying auth requests to the extracted user-service.
 *
 * Used only when auth.service.use-extracted=true (Strangler Fig pattern).
 * RestTemplate configured with timeouts to prevent cascading failures.
 */
@Configuration
public class AuthServiceProxyConfig {

    @Bean
    public RestTemplate authServiceRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
