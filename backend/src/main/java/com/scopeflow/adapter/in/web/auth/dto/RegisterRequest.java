package com.scopeflow.adapter.in.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO.
 *
 * Password must have at least 8 chars, one uppercase, one digit, one special char.
 */
public record RegisterRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must contain at least one uppercase letter, one digit, and one special character"
        )
        String password,

        @NotBlank
        @Size(max = 255)
        String fullName,

        @Pattern(regexp = "^\\+\\d{10,15}$", message = "Phone must be in E.164 format")
        String phone
) {}
