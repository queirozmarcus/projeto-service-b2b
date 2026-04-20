package com.scopeflow.user.adapter.in.web;

import com.scopeflow.user.domain.shared.InvalidValueObjectException;
import com.scopeflow.user.domain.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Global exception handler for user-service REST endpoints.
 *
 * Implements RFC 9457: Problem Details for HTTP APIs.
 * Maintains parity with monolith error codes (USER-010 to USER-013, AUTH-401).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String PROBLEM_BASE_URL = "https://api.scopeflow.com/errors/";

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ProblemDetail> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create(PROBLEM_BASE_URL + "email-already-registered"));
        pd.setTitle("Email Already Registered");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(
            InvalidCredentialsException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create(PROBLEM_BASE_URL + "invalid-credentials"));
        pd.setTitle("Invalid Credentials");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            UserNotFoundException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        pd.setType(URI.create(PROBLEM_BASE_URL + "user-not-found"));
        pd.setTitle("User Not Found");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            DuplicateEmailException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        pd.setType(URI.create(PROBLEM_BASE_URL + "duplicate-email"));
        pd.setTitle("Duplicate Email");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(InvalidInvitedByUserException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInvitedByUser(
            InvalidInvitedByUserException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(PROBLEM_BASE_URL + "invalid-invited-by-user"));
        pd.setTitle("Invalid Invited By User");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRole(
            InvalidRoleException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(PROBLEM_BASE_URL + "invalid-role"));
        pd.setTitle("Invalid Role");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Handle invalid value object (VO-001).
     *
     * Thrown when a domain value object fails validation during construction.
     * Maps to HTTP 400 Bad Request with RFC 9457 Problem Details.
     */
    @ExceptionHandler(InvalidValueObjectException.class)
    public ResponseEntity<ProblemDetail> handleInvalidValueObject(
            InvalidValueObjectException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(PROBLEM_BASE_URL + "invalid-value-object"));
        pd.setTitle("Invalid Value Object");
        pd.setDetail(ex.getMessage());
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        pd.setProperty("vo_type", ex.getVoType());
        addCustomProperties(pd, ex.getErrorCode());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setType(URI.create(PROBLEM_BASE_URL + "validation-error"));
        pd.setTitle("Validation Error");
        pd.setDetail("Request validation failed");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

        var violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> java.util.Map.of(
                        "field", fe.getField(),
                        "rejected_value", fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : "null",
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        pd.setProperty("violations", violations);
        addCustomProperties(pd, "VALIDATION-400");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ProblemDetail> handleSecurityException(
            SecurityException ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        pd.setType(URI.create(PROBLEM_BASE_URL + "unauthorized"));
        pd.setTitle("Unauthorized");
        pd.setDetail("Authentication required");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(pd, "AUTH-401");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(Exception ex, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setType(URI.create(PROBLEM_BASE_URL + "internal-server-error"));
        pd.setTitle("Internal Server Error");
        pd.setDetail("An unexpected error occurred");
        pd.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        logger.error("Unhandled exception", ex);
        String errorId = UUID.randomUUID().toString();
        pd.setProperty("error_code", "INTERNAL-500");
        pd.setProperty("error_id", errorId);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private void addCustomProperties(ProblemDetail pd, String errorCode) {
        pd.setProperty("error_code", errorCode);
        pd.setProperty("error_id", UUID.randomUUID().toString());
        pd.setProperty("timestamp", Instant.now());
    }
}
