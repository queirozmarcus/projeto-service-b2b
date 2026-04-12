package com.scopeflow.adapter.in.web.user;

import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * User management endpoints.
 *
 * Path: /api/v1/users
 * Supports workspace invite flow: lookup by email, create invited user.
 * All endpoints require authentication (JWT).
 *
 * All requests are proxied to user-service.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private final RestTemplate authServiceRestTemplate;

    public UserController(RestTemplate authServiceRestTemplate) {
        this.authServiceRestTemplate = authServiceRestTemplate;
    }

    /**
     * GET /api/v1/users/by-email/{email}
     * Find user by email address.
     *
     * Used by workspace invite flow to check if user exists before creating.
     *
     * @param email user's email address
     * @return UserResponse if found
     */
    @GetMapping("/by-email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<?> getByEmail(@PathVariable String email, HttpServletRequest request) {
        log.info("Proxying getByEmail to user-service: email={}", email);
        return proxyGetWithAuth("/users/by-email/" + email, request);
    }

    /**
     * POST /api/v1/users/invited
     * Create a new invited (INACTIVE) user.
     *
     * Used by workspace invite flow when inviting a user who does not exist yet.
     * User remains INACTIVE until they accept invite and set password.
     *
     * @param request create invited user request
     * @return UserResponse with INACTIVE status
     */
    @PostMapping("/invited")
    @Operation(summary = "Create invited user")
    public ResponseEntity<?> createInvited(@Valid @RequestBody CreateInvitedUserRequest request, HttpServletRequest httpRequest) {
        log.info("Proxying createInvited to user-service: email={}", request.email());
        return proxyPostWithAuth("/users/invited", request, httpRequest);
    }

    // ============ Proxy helpers ============

    private ResponseEntity<?> proxyGetWithAuth(String path, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.GET, entity, String.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("user-service returned error: status={}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }

    private ResponseEntity<?> proxyPostWithAuth(String path, Object body, HttpServletRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null) {
                headers.set("Authorization", authHeader);
            }
            HttpEntity<Object> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = authServiceRestTemplate.exchange(
                    authServiceUrl + path, HttpMethod.POST, entity, String.class
            );
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            log.warn("user-service returned error: status={}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
