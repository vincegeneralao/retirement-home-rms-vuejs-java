package com.rms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/login.
 *
 * <p>A blank password is accepted here on purpose: an administrator can reset a worker's
 * password to the empty string, and that worker must still be able to sign in. Credential
 * strength is enforced where passwords are created or changed, never at the gate.
 */
public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 80)
        String username,

        @NotNull(message = "password is required")
        @Size(max = 100)
        String password
) {
}
