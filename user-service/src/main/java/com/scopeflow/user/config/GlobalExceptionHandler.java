package com.scopeflow.user.config;

import com.scopeflow.user.domain.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

/**
 * Global exception handler for User Service.
 *
 * Implements RFC 9457: Problem Details for HTTP APIs.
 * Ensures consistent error responses across all endpoints.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String PROBLEM_BASE_URL = "https://api.scopeflow.com/errors/";

    // ============ Auth Exceptions ============

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

    // ============ User Domain Exceptions ============

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(
            UserNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "user-not-found"));
        problemDetail.setTitle("User Not Found");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(
            DuplicateEmailException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "duplicate-email"));
        problemDetail.setTitle("Duplicate Email");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

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

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    @ExceptionHandler(InvalidInvitedByUserException.class)
    public ResponseEntity<ProblemDetail> handleInvalidInvitedByUser(
            InvalidInvitedByUserException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-invited-by-user"));
        problemDetail.setTitle("Invalid Invited By User");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<ProblemDetail> handleInvalidRole(
            InvalidRoleException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "invalid-role"));
        problemDetail.setTitle("Invalid Role");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, ex.getErrorCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ============ Security Exceptions ============

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "access-denied"));
        problemDetail.setTitle("Access Denied");
        problemDetail.setDetail(ex.getMessage());
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, "AUTH-403");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationError(
            AuthenticationCredentialsNotFoundException ex,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "unauthorized"));
        problemDetail.setTitle("Unauthorized");
        problemDetail.setDetail("Authentication required");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        addCustomProperties(problemDetail, "AUTH-401");

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    // ============ Validation Errors ============

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setType(URI.create(PROBLEM_BASE_URL + "validation-error"));
        problemDetail.setTitle("Validation Error");
        problemDetail.setDetail("Request validation failed");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));

        var violations = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> java.util.Map.of(
                        "field", fe.getField(),
                        "rejected_value", fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : "null",
                        "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        problemDetail.setProperty("violations", violations);
        addCustomProperties(problemDetail, "VALIDATION-400");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    // ============ Generic Exception ============

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

        log.error("Unhandled exception", ex);

        String errorId = UUID.randomUUID().toString();
        problemDetail.setProperty("error_code", "INTERNAL-500");
        problemDetail.setProperty("error_id", errorId);
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    // ============ Helper Methods ============

    private void addCustomProperties(ProblemDetail problemDetail, String errorCode) {
        problemDetail.setProperty("error_code", errorCode);
        problemDetail.setProperty("error_id", UUID.randomUUID().toString());
        problemDetail.setProperty("timestamp", Instant.now());
    }
}
