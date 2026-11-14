package com.rms.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.rms.error.ApiException;

class TimeUtilTest {

    private static final ZoneId BRISBANE = ZoneId.of("Australia/Brisbane");

    @Test
    void dayBoundsAreHalfOpenMidnights() {
        TimeUtil.DayBounds b = TimeUtil.dayBounds(LocalDate.of(2026, 3, 5));
        assertThat(b.start()).isEqualTo(LocalDate.of(2026, 3, 5).atStartOfDay());
        assertThat(b.end()).isEqualTo(LocalDate.of(2026, 3, 6).atStartOfDay());
    }

    @Test
    void resolveDayParsesIso() {
        assertThat(TimeUtil.resolveDay("2026-03-05", BRISBANE)).isEqualTo(LocalDate.of(2026, 3, 5));
    }

    @Test
    void resolveDayDefaultsToToday() {
        assertThat(TimeUtil.resolveDay(null, BRISBANE)).isEqualTo(LocalDate.now(BRISBANE));
        assertThat(TimeUtil.resolveDay("  ", BRISBANE)).isEqualTo(LocalDate.now(BRISBANE));
    }

    @Test
    void resolveDayRejectsGarbage() {
        assertThatThrownBy(() -> TimeUtil.resolveDay("05/03/2026", BRISBANE))
                .isInstanceOf(ApiException.class)
                .hasMessage("date must be formatted as YYYY-MM-DD");
    }
}
