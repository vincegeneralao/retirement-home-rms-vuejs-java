package com.rms.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/worker/password/change.
 *
 * <p>{@code currentPassword} may be blank: an administrator can reset a worker to the empty
 * password, and the worker then has to present that blank value to prove ownership.
 */
public record ChangePasswordRequest(
        @NotNull(message = "currentPassword is required")
        @Size(max = 100)
        String currentPassword,

        @NotNull(message = "newPassword is required")
        @Size(min = 6, max = 100, message = "newPassword must be between 6 and 100 characters")
        String newPassword
) {
}
