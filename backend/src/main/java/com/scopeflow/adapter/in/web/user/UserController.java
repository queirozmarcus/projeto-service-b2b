package com.scopeflow.adapter.in.web.user;

import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.adapter.out.userservice.AuthProxyAdapter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * User management endpoints.
 *
 * Path: /api/v1/users
 * Supports workspace invite flow: lookup by email, create invited user.
 * All endpoints require authentication (JWT).
 *
 * All requests are proxied to user-service via AuthProxyAdapter (circuit breaker + retry).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final AuthProxyAdapter authProxyAdapter;

    public UserController(AuthProxyAdapter authProxyAdapter) {
        this.authProxyAdapter = authProxyAdapter;
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
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        String path = UriComponentsBuilder.fromPath("/users/by-email/{email}")
                .buildAndExpand(email)
                .toUriString();
        return authProxyAdapter.proxy(path, HttpMethod.GET, null, headers);
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null) {
            headers.set("Authorization", authHeader);
        }
        return authProxyAdapter.proxy("/users/invited", HttpMethod.POST, request, headers);
    }
}
