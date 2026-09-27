package com.example.shopping.upload.service;

import com.example.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberUploadLimiterTest {

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
    private final MemberUploadLimiter limiter = new MemberUploadLimiter(3, Duration.ofMinutes(10), clock);

    @Test
    void rejectsUploadsBeyondLimitWithinWindow() {
        limiter.acquire(1L);
        limiter.acquire(1L);
        limiter.acquire(1L);

        assertThatThrownBy(() -> limiter.acquire(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void limitsAreTrackedPerMember() {
        limiter.acquire(1L);
        limiter.acquire(1L);
        limiter.acquire(1L);

        assertThatCode(() -> limiter.acquire(2L)).doesNotThrowAnyException();
    }

    @Test
    void oldUploadsSlideOutOfTheWindow() {
        limiter.acquire(1L);
        limiter.acquire(1L);
        limiter.acquire(1L);
        clock.now = clock.now.plus(Duration.ofMinutes(10));

        assertThatCode(() -> limiter.acquire(1L)).doesNotThrowAnyException();
    }
}
