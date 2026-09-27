package com.example.shopping.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class SlidingWindowRateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-27T08:00:00Z");

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
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, Duration.ofMinutes(10), clock);

    @Test
    void allowsUpToMaxEventsPerKey() {
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void rejectedAttemptsDoNotExtendTheWindow() {
        limiter.tryAcquire("a");
        clock.now = clock.now.plus(Duration.ofMinutes(5));
        limiter.tryAcquire("a");
        limiter.tryAcquire("a");

        // 第一次在 10 分鐘後過期,就又有一個名額
        clock.now = clock.now.plus(Duration.ofMinutes(5));
        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
    }
}
