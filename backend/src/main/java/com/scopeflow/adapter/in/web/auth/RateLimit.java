package com.scopeflow.adapter.in.web.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as rate-limited.
 * Enforced by {@link RateLimitInterceptor}: 5 attempts per IP per 5 minutes.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
}
