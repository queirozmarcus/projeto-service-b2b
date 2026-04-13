package com.scopeflow.adapter.in.web.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO.
 */
public record LoginRequest(
        @NotBlank @Email
        String email,

        @NotBlank
        String password
) {}
