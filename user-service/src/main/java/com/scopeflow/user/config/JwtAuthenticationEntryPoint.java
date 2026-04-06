package com.scopeflow.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom AuthenticationEntryPoint that returns 401 with RFC 9457 Problem Details
 * when authentication fails (invalid/expired JWT).
 *
 * Without this, Spring Security returns 403 Forbidden by default.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        Map<String, Object> problemDetails = new HashMap<>();
        problemDetails.put("type", "about:blank");
        problemDetails.put("title", "Unauthorized");
        problemDetails.put("status", 401);
        problemDetails.put("detail", "Authentication failed: " + authException.getMessage());
        problemDetails.put("errorCode", "AUTH-001");
        problemDetails.put("errorId", UUID.randomUUID().toString());
        problemDetails.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), problemDetails);
    }
}
