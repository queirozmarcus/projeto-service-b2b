package com.scopeflow.user.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for contract tests.
 *
 * Bypasses all authentication so Spring Cloud Contract tests can verify
 * HTTP contracts without real JWT validation. The contract tests for
 * authenticated endpoints use a test Bearer token that is mocked in
 * ContractVerifierBase to populate the SecurityContext via JwtAuthenticationFilter.
 *
 * The 401 "unauthorized" contract (get-user-me-unauthorized.yml) is tested
 * separately in AuthControllerIntegrationTest where real security applies.
 * Here it is verified at the filter level via the mocked JwtAuthenticationFilter.
 */
@TestConfiguration
@EnableWebSecurity
public class ContractVerifierSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}
