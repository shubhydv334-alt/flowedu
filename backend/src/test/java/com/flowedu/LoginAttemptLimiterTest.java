package com.flowedu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptLimiterTest {

    /** A clock the test can move forward. */
    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private MutableClock clock;
    private LoginAttemptLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        limiter = new LoginAttemptLimiter(clock);
    }

    @Test
    void blocksAfterMaxFailures() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES - 1; i++) limiter.recordFailure("a");
        assertThat(limiter.isBlocked("a")).isFalse();
        limiter.recordFailure("a");
        assertThat(limiter.isBlocked("a")).isTrue();
    }

    @Test
    void keysAreIndependent() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) limiter.recordFailure("a");
        assertThat(limiter.isBlocked("b")).isFalse();
    }

    @Test
    void unblocksAfterTheWindowPasses() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) limiter.recordFailure("a");
        clock.advance(LoginAttemptLimiter.WINDOW.plusSeconds(1));
        assertThat(limiter.isBlocked("a")).isFalse();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        for (int i = 0; i < LoginAttemptLimiter.MAX_FAILURES; i++) limiter.recordFailure("a");
        limiter.reset("a");
        assertThat(limiter.isBlocked("a")).isFalse();
    }
}
