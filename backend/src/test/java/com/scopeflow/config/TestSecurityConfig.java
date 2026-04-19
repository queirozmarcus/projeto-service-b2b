package com.scopeflow.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration: permits all requests.
 *
 * Used in @WebMvcTest to bypass full JWT authentication setup.
 * Controllers under test still validate bean constraints (@Valid).
 *
 * @MockBean declarations here register mocks for JwtAuthenticationFilter dependencies
 * that are @Component but not part of the web slice — avoids NoSuchBeanDefinitionException
 * across all @WebMvcTest classes that @Import this config.
 *
 * Usage:
 * {@code @Import(TestSecurityConfig.class)}
 */
@TestConfiguration
@MockBean(JwtService.class)
@MockBean(UserStatusCacheService.class)
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
