package com.scopeflow.user.adapter.in.web;

import com.scopeflow.user.domain.exception.*;
import com.scopeflow.user.domain.shared.InvalidValueObjectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler — Unit Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest mockRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        mockRequest = mock(WebRequest.class);
        when(mockRequest.getDescription(false)).thenReturn("uri=/api/v1/test");
    }

    @Test
    @DisplayName("Should handle InvalidValueObjectException with HTTP 400")
    void shouldHandleInvalidValueObjectException_withHttp400() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format: not-an-email");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should return correct ProblemDetail type for InvalidValueObjectException")
    void shouldReturnCorrectType_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getType()).isEqualTo(URI.create("https://api.scopeflow.com/errors/invalid-value-object"));
    }

    @Test
    @DisplayName("Should return correct title for InvalidValueObjectException")
    void shouldReturnCorrectTitle_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getTitle()).isEqualTo("Invalid Value Object");
    }

    @Test
    @DisplayName("Should return error code VO-001 for InvalidValueObjectException")
    void shouldReturnErrorCodeVO001_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "VO-001");
    }

    @Test
    @DisplayName("Should include errorId for InvalidValueObjectException")
    void shouldIncludeErrorId_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsKey("error_id");

        String errorId = (String) pd.getProperties().get("error_id");
        assertThat(errorId).isNotNull();
        // Validate UUID format
        assertThat(UUID.fromString(errorId)).isNotNull();
    }

    @Test
    @DisplayName("Should include timestamp for InvalidValueObjectException")
    void shouldIncludeTimestamp_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsKey("timestamp");

        Instant timestamp = (Instant) pd.getProperties().get("timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("Should include detail message for InvalidValueObjectException")
    void shouldIncludeDetailMessage_forInvalidValueObjectException() {
        // Given
        String expectedMessage = "Invalid email format: not-an-email";
        var exception = new InvalidValueObjectException("Email", expectedMessage);

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getDetail()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should include vo_type property for InvalidValueObjectException")
    void shouldIncludeVoTypeProperty_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("vo_type", "Email");
    }

    @Test
    @DisplayName("Should include instance URI for InvalidValueObjectException")
    void shouldIncludeInstanceUri_forInvalidValueObjectException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Invalid email format");

        // When
        var response = handler.handleInvalidValueObject(exception, mockRequest);

        // Then
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getInstance()).isEqualTo(URI.create("/api/v1/test"));
    }

    @Test
    @DisplayName("Should handle different voType values in InvalidValueObjectException")
    void shouldHandleDifferentVoTypes_inInvalidValueObjectException() {
        // Given
        var emailException = new InvalidValueObjectException("Email", "Invalid email");
        var passwordException = new InvalidValueObjectException("PasswordHash", "Weak password");

        // When
        var emailResponse = handler.handleInvalidValueObject(emailException, mockRequest);
        var passwordResponse = handler.handleInvalidValueObject(passwordException, mockRequest);

        // Then
        assertThat(emailResponse.getBody().getProperties()).containsEntry("vo_type", "Email");
        assertThat(passwordResponse.getBody().getProperties()).containsEntry("vo_type", "PasswordHash");

        // Both should have same error code
        assertThat(emailResponse.getBody().getProperties()).containsEntry("error_code", "VO-001");
        assertThat(passwordResponse.getBody().getProperties()).containsEntry("error_code", "VO-001");
    }

    @Test
    @DisplayName("Should handle EmailAlreadyRegisteredException with HTTP 409")
    void shouldHandleEmailAlreadyRegisteredException_withHttp409() {
        // Given
        var exception = new EmailAlreadyRegisteredException("test@example.com");

        // When
        var response = handler.handleEmailAlreadyRegistered(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "USER-010");
    }

    @Test
    @DisplayName("Should handle InvalidCredentialsException with HTTP 401")
    void shouldHandleInvalidCredentialsException_withHttp401() {
        // Given
        var exception = new InvalidCredentialsException();

        // When
        var response = handler.handleInvalidCredentials(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "AUTH-401");
    }

    @Test
    @DisplayName("Should handle UserNotFoundException with HTTP 404")
    void shouldHandleUserNotFoundException_withHttp404() {
        // Given
        var exception = new UserNotFoundException(UUID.randomUUID());

        // When
        var response = handler.handleUserNotFound(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "USER-011");
    }

    @Test
    @DisplayName("Should handle DuplicateEmailException with HTTP 409")
    void shouldHandleDuplicateEmailException_withHttp409() {
        // Given
        var exception = new DuplicateEmailException("test@example.com");

        // When
        var response = handler.handleDuplicateEmail(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "USER-012");
    }

    @Test
    @DisplayName("Should handle generic Exception with HTTP 500")
    void shouldHandleGenericException_withHttp500() {
        // Given
        var exception = new RuntimeException("Unexpected error");

        // When
        var response = handler.handleGenericException(exception, mockRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail pd = response.getBody();
        assertThat(pd).isNotNull();
        assertThat(pd.getProperties()).containsEntry("error_code", "INTERNAL-500");
        assertThat(pd.getTitle()).isEqualTo("Internal Server Error");
    }
}
