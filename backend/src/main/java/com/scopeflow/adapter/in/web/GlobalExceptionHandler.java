package com.scopeflow.adapter.in.web;

import com.scopeflow.core.domain.user.EmailAlreadyRegisteredException;
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
