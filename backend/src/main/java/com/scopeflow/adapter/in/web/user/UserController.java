package com.scopeflow.adapter.in.web.user;

import com.scopeflow.adapter.in.web.user.dto.CreateInvitedUserRequest;
import com.scopeflow.adapter.in.web.user.dto.UserResponse;
import com.scopeflow.core.domain.user.*;
import com.scopeflow.core.domain.workspace.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * User management endpoints.
 *
 * Path: /api/v1/users
 * Supports workspace invite flow: lookup by email, create invited user.
 * All endpoints require authentication (JWT).
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management operations")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    /**
     * Strangler Fig feature flag: when true, proxies user requests to user-service.
     */
    @Value("${auth.service.use-extracted:false}")
    private boolean useExtractedAuthService;

    @Value("${auth.service.url:http://user-service:8081/api/v1}")
    private String authServiceUrl;

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate authServiceRestTemplate;

    public UserController(UserService userService, PasswordEncoder passwordEncoder, RestTemplate authServiceRestTemplate) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
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
     * @throws UserNotFoundException if user not found (404)
     */
    @GetMapping("/by-email/{email}")
    @Operation(summary = "Get user by email")
    public ResponseEntity<?> getByEmail(@PathVariable String email, HttpServletRequest request) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying getByEmail to user-service: email={}", email);
            return proxyGetWithAuth("/users/by-email/" + email, request);
        }

        Email emailVO = new Email(email);
        User user = userService.getUserByEmail(emailVO)
                .orElseThrow(() -> new UserNotFoundException(emailVO));

        log.info("User found by email: userId={}, email={}", user.getId().value(), email);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * POST /api/v1/users/invited
     * Create a new invited (INACTIVE) user.
     *
     * Used by workspace invite flow when inviting a user who does not exist yet.
     * User remains INACTIVE until they accept invite and set password.
     *
     * Validations:
     * - Email must not already exist (409)
     * - Invited by user must be valid (400)
     * - Role must be valid (400)
     *
     * @param request create invited user request
     * @return UserResponse with INACTIVE status
     * @throws DuplicateEmailException if email exists (409)
     * @throws InvalidInvitedByUserException if invitedBy user not found (400)
     */
    @PostMapping("/invited")
    @Operation(summary = "Create invited user")
    public ResponseEntity<?> createInvited(@Valid @RequestBody CreateInvitedUserRequest request, HttpServletRequest httpRequest) {
        if (useExtractedAuthService) {
            log.info("[StranglerFig] Proxying createInvited to user-service: email={}", request.email());
            return proxyPostWithAuth("/users/invited", request, httpRequest);
        }

        Email email = new Email(request.email());

        if (userService.getUserByEmail(email).isPresent()) {
            throw new DuplicateEmailException(email);
        }

        UserId invitedByUserId = new UserId(request.invitedByUserId());
        userService.getUserById(invitedByUserId)
                .orElseThrow(() -> new InvalidInvitedByUserException(invitedByUserId));

        validateRole(request.role());

        UserId newUserId = UserId.generate();
        String tempPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        PasswordHash tempHash = new PasswordHash(tempPasswordHash);
        String displayName = extractDisplayNameFromEmail(email.normalized());

        UserInactive invitedUser = User.createInvited(newUserId, email, tempHash, displayName);
        userService.saveInvitedUser(invitedUser);

        log.info("Invited user created: userId={}, email={}, invitedBy={}, role={}",
                newUserId.value(), email.normalized(), request.invitedByUserId(), request.role());

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(invitedUser));
    }

    // ============ Private Helpers ============

    /**
     * Validate role is allowed for invited users.
     * Business rule: invited users can only be MEMBER or ADMIN, not OWNER.
     */
    private void validateRole(Role role) {
        if (role == Role.OWNER) {
            throw new InvalidRoleException("Cannot invite user with OWNER role. Use workspace creation instead.");
        }
    }

    /**
     * Extract display name from email address.
     * Example: "john.doe@example.com" -> "John Doe"
     */
    private String extractDisplayNameFromEmail(String email) {
        String localPart = email.split("@")[0];
        return localPart.replace(".", " ").replace("_", " ")
                .chars()
                .collect(StringBuilder::new,
                        (sb, c) -> {
                            if (sb.isEmpty() || sb.charAt(sb.length() - 1) == ' ') {
                                sb.append(Character.toUpperCase(c));
                            } else {
                                sb.append((char) c);
                            }
                        },
                        StringBuilder::append)
                .toString();
    }

    // ============ Strangler Fig: proxy helpers ============

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
            log.warn("[StranglerFig] user-service returned error: status={}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[StranglerFig] Failed to proxy to user-service", e);
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
            log.warn("[StranglerFig] user-service returned error: status={}", e.getStatusCode());
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[StranglerFig] Failed to proxy to user-service", e);
            throw new RuntimeException("User service unavailable", e);
        }
    }
}
