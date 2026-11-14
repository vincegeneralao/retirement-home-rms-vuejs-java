package com.rms.dto;

import com.rms.entity.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body of POST /api/admin/workers/{workerId}/availability. */
public record AvailabilityRequest(
        @NotNull(message = "dayOfWeek is required")
        DayOfWeek dayOfWeek,

        @NotBlank(message = "startTime is required")
        String startTime,

        @NotBlank(message = "endTime is required")
        String endTime
) {
}
