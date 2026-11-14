package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/admin/workers. */
public record CreateWorkerRequest(
        @NotBlank(message = "username is required")
        @Size(min = 3, max = 80, message = "username must be between 3 and 80 characters")
        @Pattern(regexp = "^[a-z0-9._-]+$",
                message = "username may only contain lowercase letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
        String password
) {
}
