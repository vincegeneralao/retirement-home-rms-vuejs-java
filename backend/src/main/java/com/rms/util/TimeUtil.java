package com.rms.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.rms.error.ApiErrors;

/** Day-window helpers operating in site-local wall-clock time. */
public final class TimeUtil {

    private TimeUtil() {
    }

    /** A half-open day window [day 00:00, next day 00:00). */
    public record DayBounds(LocalDateTime start, LocalDateTime end) {
    }

    public static DayBounds dayBounds(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        return new DayBounds(start, start.plusDays(1));
    }

    /** Parse an optional YYYY-MM-DD query value, defaulting to today in the site timezone. */
    public static LocalDate resolveDay(String iso, ZoneId zone) {
        if (iso == null || iso.isBlank()) {
            return LocalDate.now(zone);
        }
        try {
            return LocalDate.parse(iso.trim());
        } catch (RuntimeException ex) {
            throw ApiErrors.validation("date must be formatted as YYYY-MM-DD");
        }
    }
}
