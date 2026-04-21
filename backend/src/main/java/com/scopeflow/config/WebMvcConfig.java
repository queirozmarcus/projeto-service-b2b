package com.scopeflow.config;

import com.scopeflow.adapter.in.web.auth.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration.
 * Registra interceptors globais, incluindo o de rate limiting para endpoints de auth.
 *
 * O rate limiter pode ser desabilitado via {@code auth.rate-limit.enabled=false}
 * (usado em testes para evitar interferência entre test classes no contexto cacheado).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${auth.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (rateLimitEnabled) {
            registry.addInterceptor(new RateLimitInterceptor())
                    .addPathPatterns("/api/v1/auth/**");
        }
    }
}
