package com.scopeflow.adapter.in.web;

import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.user.EmailAlreadyRegisteredException;
import com.scopeflow.core.domain.user.InvalidCredentialsException;
import com.scopeflow.core.domain.workspace.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Global exception handler for all REST endpoints.
 *
 * Implements RFC 9457: Problem Details for HTTP APIs
 * (https://www.rfc-editor.org/rfc/rfc9457)
 *
 * Converts domain exceptions to HTTP problem details responses.
 * Ensures consistent error responses across all APIs.
 *
 * Example error response:
 * {
 *   "type": "https://api.example.com/errors/email-already-registered",
 *   "title": "Email Already Registered",
 *   "status": 409,
 *   "detail": "Email 'user@example.com' is already registered",
 *   "instance": "/api/v1/auth/register",
 *   "error_code": "USER-001",
 *   "error_id": "550e8400-e29b-41d4-a716-446655440000"
 * }
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://api.scopeflow.com/errors/";

    /**
     * Handle email uniqueness violation.
     */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "email-already-registered"));
        problemDetail.setTitle("Email Already Registered");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle workspace name uniqueness violation.
     */
    @ExceptionHandler(WorkspaceNameAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleWorkspaceNameAlreadyExists(
            WorkspaceNameAlreadyExistsException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "workspace-name-already-exists"));
        problemDetail.setTitle("Workspace Name Already Exists");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle workspace not found.
     */
    @ExceptionHandler(WorkspaceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleWorkspaceNotFound(
            WorkspaceNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "workspace-not-found"));
        problemDetail.setTitle("Workspace Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    /**
     * Handle invariant violation: cannot remove last OWNER.
     */
    @ExceptionHandler(CannotRemoveLastOwnerException.class)
    public ResponseEntity<ProblemDetail> handleCannotRemoveLastOwner(
            CannotRemoveLastOwnerException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "cannot-remove-last-owner"));
        problemDetail.setTitle("Cannot Remove Last Owner");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle member already exists violation.
     */
    @ExceptionHandler(MemberAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleMemberAlreadyExists(
            MemberAlreadyExistsException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "member-already-exists"));
        problemDetail.setTitle("Member Already Exists");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle member not found.
     */
    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleMemberNotFound(
            MemberNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "member-not-found"));
        problemDetail.setTitle("Member Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    // ============ Auth Exceptions ============

    /**
     * Handle invalid credentials (login failure).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-credentials"));
        problemDetail.setTitle("Invalid Credentials");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    // ============ Proposal Domain Exceptions ============

    /**
     * Handle proposal not found.
     */
    @ExceptionHandler(ProposalNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleProposalNotFound(
            ProposalNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "proposal-not-found"));
        problemDetail.setTitle("Proposal Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handle invalid proposal state transition.
     */
    @ExceptionHandler(InvalidProposalStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidProposalState(
            InvalidProposalStateException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-proposal-state"));
        problemDetail.setTitle("Invalid Proposal State");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handle expired approval token.
     */
    @ExceptionHandler(ApprovalTokenExpiredException.class)
    public ResponseEntity<ProblemDetail> handleApprovalTokenExpired(
            ApprovalTokenExpiredException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "approval-token-expired"));
        problemDetail.setTitle("Approval Token Expired");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    // ============ Briefing Domain Exceptions ============

    /**
     * Handle briefing session not found.
     */
    @ExceptionHandler(BriefingNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleBriefingNotFound(
            BriefingNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "briefing-not-found"));
        problemDetail.setTitle("Briefing Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemDetail);
    }

    /**
     * Handle briefing already completed (cannot modify).
     */
    @ExceptionHandler(BriefingAlreadyCompletedException.class)
    public ResponseEntity<ProblemDetail> handleBriefingAlreadyCompleted(
            BriefingAlreadyCompletedException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "briefing-already-completed"));
        problemDetail.setTitle("Briefing Already Completed");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle invalid answer (empty, too long, etc.).
     */
    @ExceptionHandler(InvalidAnswerException.class)
    public ResponseEntity<ProblemDetail> handleInvalidAnswer(
            InvalidAnswerException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-answer"));
        problemDetail.setTitle("Invalid Answer");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handle max follow-up exceeded (max 1 per question).
     */
    @ExceptionHandler(MaxFollowupExceededException.class)
    public ResponseEntity<ProblemDetail> handleMaxFollowupExceeded(
            MaxFollowupExceededException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "max-followup-exceeded"));
        problemDetail.setTitle("Max Follow-up Exceeded");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(problemDetail);
    }

    /**
     * Handle incomplete briefing (completion score < 80%).
     */
    @ExceptionHandler(IncompleteGapsException.class)
    public ResponseEntity<ProblemDetail> handleIncompleteGaps(
            IncompleteGapsException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "incomplete-gaps"));
        problemDetail.setTitle("Incomplete Briefing");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(problemDetail);
    }

    /**
     * Handle briefing already in progress (duplicate active session).
     */
    @ExceptionHandler(BriefingAlreadyInProgressException.class)
    public ResponseEntity<ProblemDetail> handleBriefingAlreadyInProgress(
            BriefingAlreadyInProgressException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "briefing-already-in-progress"));
        problemDetail.setTitle("Briefing Already In Progress");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle invalid state exception (generic briefing state violation).
     */
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ProblemDetail> handleInvalidState(
            InvalidStateException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-state"));
        problemDetail.setTitle("Invalid State");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemDetail);
    }

    /**
     * Handle access denied (workspace ownership violation).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "access-denied"));
        problemDetail.setTitle("Access Denied");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, "AUTH-403");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(problemDetail);
    }

    /**
     * Handle authentication errors (missing or invalid JWT).
     */
    @ExceptionHandler(org.springframework.security.authentication.AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationError(
            org.springframework.security.authentication.AuthenticationCredentialsNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "unauthorized"));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setDetail("Authentication required");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, "AUTH-401");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(problemDetail);
    }

    /**
     * Handle rate limit exceeded.
     *
     * Note: This is a placeholder. In production, use a proper rate limiting library
     * (e.g., Bucket4j, Spring Cloud Gateway rate limiter, or Redis-based solution).
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(
            RateLimitExceededException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.TOO_MANY_REQUESTS);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "rate-limit-exceeded"));
        problemDetail.setTitle("Rate Limit Exceeded");
        problemDetail.setDetail("Too many requests. Please try again later.");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, "RATE-429");

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(problemDetail);
    }

    /**
     * Handle validation errors (Bean Validation).
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
            org.springframework.web.bind.MethodArgumentNotValidException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setDetail("Request validation failed");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

        // Add violations list
        var violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> java.util.Map.of(
                        "field", fe.getField(),
                        "rejected_value", fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : "null",
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        problemDetail.setProperty("violations", violations);
        addCustomProperties(problemDetail, "VALIDATION-400");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(problemDetail);
    }

    /**
     * Handle generic exceptions (catch-all).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "internal-server-error"));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setDetail("An unexpected error occurred");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

        // Log full exception for debugging
        logger.error("Unhandled exception", ex);

        // Add error ID for support tickets
        String errorId = UUID.randomUUID().toString();
        problemDetail.setProperty("error_code", "INTERNAL-500");
        problemDetail.setProperty("error_id", errorId);
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemDetail);
    }

    /**
     * Add custom properties to ProblemDetail (error_code, error_id, timestamp).
     */
    private void addCustomProperties(ProblemDetail problemDetail, String errorCode) {
        problemDetail.setProperty("error_code", errorCode);
        problemDetail.setProperty("error_id", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
    }
}
