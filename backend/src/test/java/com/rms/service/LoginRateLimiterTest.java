package com.rms.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.rms.config.AppProperties;
import com.rms.error.ApiException;

class LoginRateLimiterTest {

    /** Clock the test can advance by hand. */
    private static final class TickClock extends Clock {
        private long millis;

        TickClock(long start) {
            this.millis = start;
        }

        void advance(Duration d) {
            millis += d.toMillis();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }
    }

    private static AppProperties props(int max, Duration window, Duration lockout) {
        AppProperties p = new AppProperties();
        p.getRateLimit().setMaxFailedAttempts(max);
        p.getRateLimit().setWindow(window);
        p.getRateLimit().setLockout(lockout);
        return p;
    }

    @Test
    void blocksOnceAllowanceExhausted() {
        TickClock clock = new TickClock(1_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(
                props(3, Duration.ofMinutes(10), Duration.ofMinutes(15)), clock);

        limiter.checkAllowed("k");
        limiter.recordFailure("k");
        limiter.recordFailure("k");
        limiter.checkAllowed("k"); // 2 failures < 3 allowance

        limiter.recordFailure("k"); // trips the lockout
        assertThatThrownBy(() -> limiter.checkAllowed("k"))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(429);
                    assertThat(ex.getCode()).isEqualTo("TOO_MANY_ATTEMPTS");
                    assertThat(ex.getRetryAfterSeconds()).isEqualTo(15 * 60L);
                });
    }

    @Test
    void successClearsFailureHistory() {
        TickClock clock = new TickClock(2_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(
                props(3, Duration.ofMinutes(10), Duration.ofMinutes(15)), clock);

        limiter.recordFailure("k");
        limiter.recordFailure("k");
        limiter.recordSuccess("k");
        limiter.recordFailure("k");
        limiter.recordFailure("k");
        assertThatCode(() -> limiter.checkAllowed("k")).doesNotThrowAnyException();
    }

    @Test
    void staleWindowResetsTheCount() {
        TickClock clock = new TickClock(3_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(
                props(2, Duration.ofMinutes(10), Duration.ofMinutes(15)), clock);

        limiter.recordFailure("k");
        clock.advance(Duration.ofMinutes(11)); // window elapsed
        limiter.recordFailure("k"); // counted as the first failure of a new window

        assertThatCode(() -> limiter.checkAllowed("k")).doesNotThrowAnyException();

        limiter.recordFailure("k"); // second failure of the new window → trips
        assertThatThrownBy(() -> limiter.checkAllowed("k")).isInstanceOf(ApiException.class);
    }

    @Test
    void lockoutExpires() {
        TickClock clock = new TickClock(4_000_000L);
        LoginRateLimiter limiter = new LoginRateLimiter(
                props(1, Duration.ofMinutes(10), Duration.ofMinutes(15)), clock);

        limiter.recordFailure("k"); // allowance of 1 → lockout
        assertThatThrownBy(() -> limiter.checkAllowed("k")).isInstanceOf(ApiException.class);

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));
        assertThatCode(() -> limiter.checkAllowed("k")).doesNotThrowAnyException();
    }
}
