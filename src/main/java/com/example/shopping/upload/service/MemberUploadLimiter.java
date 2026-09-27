package com.example.shopping.upload.service;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 會員上傳圖片的頻率限制(滑動視窗,記憶體內),避免被拿來當免費圖床或灌爆磁碟 */
@Component
public class MemberUploadLimiter {

    private final int maxUploads;
    private final Duration window;
    private final Clock clock;
    private final Map<Long, Deque<Instant>> uploads = new ConcurrentHashMap<>();

    @Autowired
    public MemberUploadLimiter(@Value("${app.upload.member-max-per-window:20}") int maxUploads,
                               @Value("${app.upload.member-window-minutes:10}") long windowMinutes) {
        this(maxUploads, Duration.ofMinutes(windowMinutes), Clock.systemUTC());
    }

    MemberUploadLimiter(int maxUploads, Duration window, Clock clock) {
        this.maxUploads = maxUploads;
        this.window = window;
        this.clock = clock;
    }

    /** 記錄一次上傳;超過上限丟 429 */
    public void acquire(Long memberId) {
        Instant now = clock.instant();
        Deque<Instant> timestamps = uploads.computeIfAbsent(memberId, id -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(now.minus(window))) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxUploads) {
                throw new BusinessException("上傳太頻繁,請稍後再試", HttpStatus.TOO_MANY_REQUESTS);
            }
            timestamps.addLast(now);
        }
    }
}
