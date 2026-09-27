package com.example.shopping.security;

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

class LoginAttemptServiceTest {

    /** 可以手動撥快的時鐘 */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-27T08:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
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
            return now;
        }
    }

    private final MutableClock clock = new MutableClock();
    private final LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);

    private void fail(int times) {
        for (int i = 0; i < times; i++) {
            service.recordFailure("member", "a@example.com");
        }
    }

    @Test
    void locksAfterMaxFailures() {
        fail(2);
        assertThatCode(() -> service.checkNotLocked("member", "a@example.com")).doesNotThrowAnyException();

        fail(1);
        assertThatThrownBy(() -> service.checkNotLocked("member", "a@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("15 分鐘")
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void accountKeyIsCaseInsensitive() {
        fail(3);
        assertThatThrownBy(() -> service.checkNotLocked("member", " A@Example.com "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void scopesAreIndependent() {
        fail(3);
        assertThatCode(() -> service.checkNotLocked("admin", "a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void unlocksAfterLockDurationAndStartsCountingAgain() {
        fail(3);
        clock.advance(Duration.ofMinutes(15));
        assertThatCode(() -> service.checkNotLocked("member", "a@example.com")).doesNotThrowAnyException();

        fail(1);
        assertThatCode(() -> service.checkNotLocked("member", "a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void successResetsFailureCount() {
        fail(2);
        service.recordSuccess("member", "a@example.com");
        fail(2);
        assertThatCode(() -> service.checkNotLocked("member", "a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void oldFailuresExpire_whenNotLocked() {
        fail(2);
        clock.advance(Duration.ofMinutes(16));
        fail(1);
        assertThatCode(() -> service.checkNotLocked("member", "a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void reportsRemainingMinutesRoundedUp() {
        fail(3);
        clock.advance(Duration.ofMinutes(10).plusSeconds(30));
        assertThatThrownBy(() -> service.checkNotLocked("member", "a@example.com"))
                .hasMessageContaining("5 分鐘");
    }
}
