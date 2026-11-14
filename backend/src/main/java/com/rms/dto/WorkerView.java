package com.rms.dto;

import java.util.List;

/** A support worker row in the admin directory, with today's shift count and availability. */
public record WorkerView(
        Long id,
        String fullName,
        String username,
        String role,
        boolean enabled,
        long shiftsToday,
        List<AvailabilityView> availability
) {
}
