package com.scopeflow.adapter.in.web.auth;

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
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Spring MVC interceptor que aplica rate limiting nos endpoints anotados com {@link RateLimit}.
 *
 * Política: 5 tentativas por IP a cada 5 minutos (greedy refill).
 * Em caso de excesso retorna 429 Too Many Requests com Retry-After em segundos.
 *
 * Implementação sem AOP: usa {@link HandlerInterceptor} e Bucket4j in-process.
 * Cache gerenciado por Caffeine com expiração automática de entradas inativas (10 min).
 */
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(5);

    // Cache IP → Bucket; entradas expiram 10 minutos após último acesso
    private final Map<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .<String, Bucket>build()
            .asMap();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
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
            response.addHeader("X-RateLimit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
        log.warn("Rate limit exceeded: ip={}, endpoint={}, retryAfterSeconds={}",
                clientIp, request.getRequestURI(), retryAfterSeconds);

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
        // 5 tokens por janela de 5 minutos — refill greedy (repõe todos de uma vez no fim da janela)
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_ATTEMPTS)
                .refillIntervally(MAX_ATTEMPTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Pega o primeiro IP da cadeia (IP original do cliente)
            return xForwardedFor.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
