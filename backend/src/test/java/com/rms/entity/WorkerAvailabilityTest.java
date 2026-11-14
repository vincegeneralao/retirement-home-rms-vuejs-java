package com.rms.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkerAvailabilityTest {

    private WorkerAvailability window(DayOfWeek day, String start, String end) {
        return WorkerAvailability.builder().workerId(1L).dayOfWeek(day).startTime(start).endTime(end).build();
    }

    @Test
    void detectsOverlapOnSameDay() {
        WorkerAvailability a = window(DayOfWeek.MONDAY, "08:00", "12:00");
        assertThat(a.overlaps(window(DayOfWeek.MONDAY, "11:00", "13:00"))).isTrue();
        assertThat(a.overlaps(window(DayOfWeek.MONDAY, "07:00", "08:00"))).isFalse();
        assertThat(a.overlaps(window(DayOfWeek.MONDAY, "12:00", "14:00"))).isFalse();
    }

    @Test
    void differentDaysNeverOverlap() {
        WorkerAvailability a = window(DayOfWeek.MONDAY, "08:00", "12:00");
        assertThat(a.overlaps(window(DayOfWeek.TUESDAY, "08:00", "12:00"))).isFalse();
    }

    @Test
    void dayOrderIsMondayFirst() {
        assertThat(DayOfWeek.MONDAY.order()).isEqualTo(1);
        assertThat(DayOfWeek.SUNDAY.order()).isEqualTo(7);
    }
}
