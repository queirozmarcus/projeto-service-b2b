package com.scopeflow.user.adapter.in.web.auth;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Rate limiting interceptor: 5 attempts per IP per 5 minutes.
 *
 * <p><b>IP Spoofing Protection:</b> Trusting X-Forwarded-For unconditionally allows
 * an attacker to forge any IP on each request, bypassing rate limiting entirely.
 * This interceptor only reads X-Forwarded-For when the immediate connection
 * (RemoteAddr) comes from a known trusted proxy.
 *
 * <p><b>Configuration:</b> Set {@code app.rate-limit.trusted-proxies} to the
 * comma-separated list of your proxy IPs (e.g., Traefik container IP).
 * If left empty (default), RemoteAddr is always used — safe with no proxy.
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    private final Set<String> trustedProxies;

    private final Map<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .<String, Bucket>build()
            .asMap();

    public RateLimitInterceptor(String trustedProxiesConfig) {
        this.trustedProxies = parseTrustedProxies(trustedProxiesConfig);
        if (trustedProxies.isEmpty()) {
            log.info("RateLimitInterceptor: no trusted proxies configured — using RemoteAddr always (safe default)");
        } else {
            log.info("RateLimitInterceptor: trusted proxies configured: {}", trustedProxies);
        }
    }

    private static Set<String> parseTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(config.split(","))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (!handlerMethod.hasMethodAnnotation(RateLimit.class)) {
            return true;
        }

        String clientIp = resolveClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(clientIp, ip -> buildBucket());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        log.warn("Rate limit exceeded: ip={}, endpoint={}, retryAfterSeconds={}", clientIp, request.getRequestURI(), retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("""
                {"type":"https://api.scopeflow.com/errors/rate-limit",\
                "title":"Too Many Requests",\
                "status":429,\
                "detail":"Muitas tentativas. Aguarde %d segundos antes de tentar novamente."}"""
                .formatted(retryAfterSeconds));
        return false;
    }

    private static Bucket buildBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_ATTEMPTS)
                .refillIntervally(MAX_ATTEMPTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resolves the real client IP in a spoofing-resistant way.
     *
     * <p>Strategy:
     * <ul>
     *   <li>If no trusted proxies are configured → use RemoteAddr (safe default, no proxy).</li>
     *   <li>If RemoteAddr is a trusted proxy → the proxy appended the client IP to X-Forwarded-For;
     *       take the last entry added by the trusted proxy (penultimate in the list, i.e. the entry
     *       just before the proxy itself). Falls back to RemoteAddr if the header is missing/malformed.</li>
     *   <li>If RemoteAddr is NOT a trusted proxy → request came directly (or from an unknown proxy);
     *       ignore X-Forwarded-For entirely to prevent spoofing.</li>
     * </ul>
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (trustedProxies.isEmpty()) {
            return remoteAddr;
        }

        if (!trustedProxies.contains(remoteAddr)) {
            // Direct connection or unknown proxy — never trust the header
            return remoteAddr;
        }

        // RemoteAddr is a trusted proxy: extract rightmost client IP from X-Forwarded-For.
        // Traefik appends the real client IP, so the list is: [original-client, ..., last-hop-client]
        // We want the last entry that the trusted proxy added — i.e. the rightmost one.
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] parts = xForwardedFor.split(",");
        // Rightmost entry is the IP closest to the trusted proxy — the real client.
        String clientIp = parts[parts.length - 1].strip();
        return clientIp.isEmpty() ? remoteAddr : clientIp;
    }
}
