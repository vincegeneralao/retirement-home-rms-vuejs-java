package com.rms.entity;

/**
 * Day-of-week for worker availability windows. Kept as a dedicated enum (rather than
 * java.time.DayOfWeek) so JSON serialisation is always the stable name (MONDAY..SUNDAY).
 */
public enum DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    /** 1 = Monday .. 7 = Sunday (matches the legacy DAY_ORDER sort key). */
    public int order() {
        return ordinal() + 1;
    }
}
