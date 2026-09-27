package com.example.shopping.member.mail;

import com.example.shopping.common.ratelimit.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

/**
 * 限制「忘記密碼」「重寄驗證信」這類任何人都能觸發的寄信,避免被拿來轟炸某個信箱或燒光寄信額度。
 * 以收件 email 計數(不論是否為會員),每種信件各自計算。
 */
@Component
public class EmailSendLimiter {

    private final SlidingWindowRateLimiter limiter;

    @Autowired
    public EmailSendLimiter(@Value("${app.mail.max-per-email-per-hour:3}") int maxPerHour) {
        this(maxPerHour, Duration.ofHours(1), Clock.systemUTC());
    }

    public EmailSendLimiter(int maxPerWindow, Duration window, Clock clock) {
        this.limiter = new SlidingWindowRateLimiter(maxPerWindow, window, clock);
    }

    /** @param purpose 信件種類,例如 password-reset */
    public boolean tryAcquire(String purpose, String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        return limiter.tryAcquire(purpose + ":" + normalized);
    }
}
