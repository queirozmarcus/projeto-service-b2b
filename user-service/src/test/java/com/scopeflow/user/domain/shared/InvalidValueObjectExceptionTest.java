package com.scopeflow.user.domain.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InvalidValueObjectException — Unit Tests")
class InvalidValueObjectExceptionTest {

    @Test
    @DisplayName("Should create exception with voType and message")
    void shouldCreateException_withVoTypeAndMessage() {
        // Given
        String voType = "Email";
        String message = "Invalid email format: not-an-email";

        // When
        var exception = new InvalidValueObjectException(voType, message);

        // Then
        assertThat(exception.getVoType()).isEqualTo("Email");
        assertThat(exception.getMessage()).isEqualTo("Invalid email format: not-an-email");
    }

    @Test
    @DisplayName("Should return error code VO-001")
    void shouldReturnErrorCodeVO001() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Test message");

        // When
        String errorCode = exception.getErrorCode();

        // Then
        assertThat(errorCode).isEqualTo("VO-001");
    }

    @Test
    @DisplayName("Should extend RuntimeException")
    void shouldExtendRuntimeException() {
        // Given
        var exception = new InvalidValueObjectException("Email", "Test message");

        // Then
        assertThat(exception).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should return correct voType")
    void shouldReturnCorrectVoType() {
        // Given
        var exception = new InvalidValueObjectException("PasswordHash", "Password too weak");

        // When
        String voType = exception.getVoType();

        // Then
        assertThat(voType).isEqualTo("PasswordHash");
    }

    @Test
    @DisplayName("Should return correct message")
    void shouldReturnCorrectMessage() {
        // Given
        String expectedMessage = "Invalid email format: not-an-email";
        var exception = new InvalidValueObjectException("Email", expectedMessage);

        // When
        String actualMessage = exception.getMessage();

        // Then
        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("Should handle different voType values")
    void shouldHandleDifferentVoTypeValues() {
        // Given / When
        var emailException = new InvalidValueObjectException("Email", "Invalid email");
        var passwordException = new InvalidValueObjectException("PasswordHash", "Weak password");
        var usernameException = new InvalidValueObjectException("Username", "Invalid username");

        // Then
        assertThat(emailException.getVoType()).isEqualTo("Email");
        assertThat(passwordException.getVoType()).isEqualTo("PasswordHash");
        assertThat(usernameException.getVoType()).isEqualTo("Username");

        // All should have same error code
        assertThat(emailException.getErrorCode()).isEqualTo("VO-001");
        assertThat(passwordException.getErrorCode()).isEqualTo("VO-001");
        assertThat(usernameException.getErrorCode()).isEqualTo("VO-001");
    }
}
