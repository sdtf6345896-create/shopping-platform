package com.example.shopping.upload.service;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.ratelimit.SlidingWindowRateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** 會員上傳圖片的頻率限制,避免被拿來當免費圖床或灌爆磁碟 */
@Component
public class MemberUploadLimiter {

    private final SlidingWindowRateLimiter limiter;

    @Autowired
    public MemberUploadLimiter(@Value("${app.upload.member-max-per-window:20}") int maxUploads,
                               @Value("${app.upload.member-window-minutes:10}") long windowMinutes) {
        this(maxUploads, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    MemberUploadLimiter(int maxUploads, Duration window, Clock clock) {
        this.limiter = new SlidingWindowRateLimiter(maxUploads, window, clock);
    }

    /** 記錄一次上傳;超過上限丟 429 */
    public void acquire(Long memberId) {
        if (!limiter.tryAcquire(String.valueOf(memberId))) {
            throw new BusinessException("上傳太頻繁,請稍後再試", HttpStatus.TOO_MANY_REQUESTS);
        }
    }
}
