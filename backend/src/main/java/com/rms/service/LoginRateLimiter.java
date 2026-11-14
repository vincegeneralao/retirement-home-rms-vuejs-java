package com.rms.service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rms.config.AppProperties;
import com.rms.error.ApiErrors;

/**
 * Per-key failure counter that locks out sign-in attempts once too many consecutive failures
 * arrive within a rolling window. Guards {@code POST /api/auth/login} against password
 * guessing, which the seeded demo accounts make otherwise trivial.
 *
 * <p>State is in-memory: a restart clears lockouts, which is acceptable for this API's
 * threat model and deployment (single backend container).</p>
 */
@Component
public class LoginRateLimiter {

    /** Once more than this many keys are tracked, expired entries are swept out. */
    private static final int PURGE_THRESHOLD = 10_000;

    private final AppProperties.RateLimit config;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public LoginRateLimiter(AppProperties props) {
        this(props, Clock.systemUTC());
    }

    LoginRateLimiter(AppProperties props, Clock clock) {
        this.config = props.getRateLimit();
        this.clock = clock;
    }

    /** Rejects a sign-in attempt that arrives while the key is in lockout. */
    public void checkAllowed(String key) {
        long now = clock.millis();
        Window window = windows.get(key);
        if (window != null) {
            long blockedUntil;
            synchronized (window) {
                blockedUntil = window.blockedUntil;
            }
            if (blockedUntil > now) {
                throw ApiErrors.tooManyAttempts((blockedUntil - now + 999) / 1000);
            }
        }
    }

    /** Registers a failed attempt; trips the lockout once the window's allowance is used up. */
    public void recordFailure(String key) {
        long now = clock.millis();
        purgeIfNeeded(now);
        Window window = windows.computeIfAbsent(key, k -> new Window());
        synchronized (window) {
            if (window.blockedUntil > now) {
                return; // already locked out; keep the original release time
            }
            if (now - window.windowStart > config.getWindow().toMillis()) {
                window.windowStart = now; // stale window: start counting again
                window.failures = 0;
            }
            window.failures++;
            if (window.failures >= config.getMaxFailedAttempts()) {
                window.blockedUntil = now + config.getLockout().toMillis();
                window.failures = 0;
                window.windowStart = window.blockedUntil; // fresh allowance after lockout ends
            }
        }
    }

    /** A successful sign-in clears the key's failure history. */
    public void recordSuccess(String key) {
        windows.remove(key);
    }

    private void purgeIfNeeded(long now) {
        if (windows.size() <= PURGE_THRESHOLD) {
            return;
        }
        long staleAfter = config.getWindow().toMillis() + config.getLockout().toMillis();
        windows.entrySet().removeIf(entry -> {
            Window window = entry.getValue();
            synchronized (window) {
                return window.blockedUntil <= now && now - window.windowStart > staleAfter;
            }
        });
    }

    private static final class Window {
        long windowStart;
        int failures;
        long blockedUntil;
    }
}
