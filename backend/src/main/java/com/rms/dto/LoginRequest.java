package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/auth/login. */
public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 80)
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 100, message = "password must be between 6 and 100 characters")
        String password
) {
}
