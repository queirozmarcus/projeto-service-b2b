package com.scopeflow.user.config;

import com.scopeflow.user.adapter.in.web.auth.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration.
 * Registers rate limiting interceptor for auth endpoints.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.rate-limit.trusted-proxies:}")
    private String trustedProxies;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor(trustedProxies))
                .addPathPatterns("/auth/**");
    }
}
