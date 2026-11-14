package com.rms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/admin/open-shifts. */
public record CreateOpenShiftRequest(
        @NotNull(message = "clientId is required")
        @Min(value = 1, message = "clientId is required")
        Long clientId,

        @NotNull(message = "date must be formatted as YYYY-MM-DD")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "date must be formatted as YYYY-MM-DD")
        String date,

        @NotNull(message = "startTime must be formatted as HH:mm")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "startTime must be formatted as HH:mm")
        String startTime,

        @NotNull(message = "endTime must be formatted as HH:mm")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "endTime must be formatted as HH:mm")
        String endTime,

        @Size(max = 500, message = "notes must be at most 500 characters")
        String notes
) {
}
