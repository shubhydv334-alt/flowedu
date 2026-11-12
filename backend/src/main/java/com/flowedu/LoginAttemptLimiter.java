package com.flowedu;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Slows down password guessing: after MAX_FAILURES failed logins for the same key within WINDOW the key is
 * blocked until the window ends. A successful login clears the counter.
 * State is kept in memory, so it is per instance and resets on restart; put a rate limiter in front of the
 * service when running several replicas.
 */
@Component
public class LoginAttemptLimiter {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int PURGE_THRESHOLD = 10_000;

    private record Entry(int failures, Instant windowStart) {}

    private final ConcurrentHashMap<String, Entry> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Entry e = attempts.get(key);
        if (e == null) return false;
        if (expired(e)) {
            attempts.remove(key, e);
            return false;
        }
        return e.failures() >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        if (attempts.size() > PURGE_THRESHOLD) {
            attempts.values().removeIf(this::expired);
        }
        Instant now = clock.instant();
        attempts.merge(key, new Entry(1, now),
                (old, fresh) -> expired(old) ? fresh : new Entry(old.failures() + 1, old.windowStart()));
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean expired(Entry e) {
        return clock.instant().isAfter(e.windowStart().plus(WINDOW));
    }
}
