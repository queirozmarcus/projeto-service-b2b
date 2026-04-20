package com.scopeflow.core.domain.user;

import com.scopeflow.core.domain.common.InvalidValueObjectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Email Value Object")
class EmailTest {

    @Test
    @DisplayName("should create valid email")
    void shouldCreateValidEmail() {
        // When
        Email email = new Email("user@example.com");

        // Then
        assertThat(email.value()).isEqualTo("user@example.com");
        assertThat(email.normalized()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should normalize email to lowercase")
    void shouldNormalizeToLowercase() {
        // When
        Email email = new Email("User@Example.COM");

        // Then
        assertThat(email.value()).isEqualTo("User@Example.COM");
        assertThat(email.normalized()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when format is invalid")
    void shouldThrowInvalidValueObjectException_whenFormatInvalid() {
        // When/Then
        assertThatThrownBy(() -> new Email("invalid-email"))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Invalid email format")
                .extracting("voType", "errorCode")
                .containsExactly("Email", "VO-001");
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when missing @ symbol")
    void shouldThrowException_whenMissingAtSymbol() {
        // When/Then
        assertThatThrownBy(() -> new Email("userexample.com"))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when missing domain")
    void shouldThrowException_whenMissingDomain() {
        // When/Then
        assertThatThrownBy(() -> new Email("user@"))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when email is empty")
    void shouldThrowException_whenEmpty() {
        // When/Then
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Email cannot be empty");
    }

    @Test
    @DisplayName("should throw InvalidValueObjectException when email is blank")
    void shouldThrowException_whenBlank() {
        // When/Then
        assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(InvalidValueObjectException.class)
                .hasMessageContaining("Email cannot be empty");
    }

    @Test
    @DisplayName("should throw NullPointerException when email is null")
    void shouldThrowException_whenNull() {
        // When/Then
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Email value cannot be null");
    }

    @Test
    @DisplayName("should accept valid email with subdomains")
    void shouldAcceptEmailWithSubdomains() {
        // When
        Email email = new Email("user@mail.example.com");

        // Then
        assertThat(email.value()).isEqualTo("user@mail.example.com");
    }

    @Test
    @DisplayName("should accept valid email with plus addressing")
    void shouldAcceptEmailWithPlusAddressing() {
        // When
        Email email = new Email("user+tag@example.com");

        // Then
        assertThat(email.value()).isEqualTo("user+tag@example.com");
    }

    @Test
    @DisplayName("should accept valid email with dots and hyphens")
    void shouldAcceptEmailWithDotsAndHyphens() {
        // When
        Email email = new Email("first.last@my-company.co.uk");

        // Then
        assertThat(email.value()).isEqualTo("first.last@my-company.co.uk");
    }
}
