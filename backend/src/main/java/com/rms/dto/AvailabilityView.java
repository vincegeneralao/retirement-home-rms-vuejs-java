package com.rms.dto;

import com.rms.entity.DayOfWeek;

/** An availability window as returned to the client. */
public record AvailabilityView(
        Long id,
        DayOfWeek dayOfWeek,
        String startTime,
        String endTime
) {
}
