package com.example.shopping.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 記憶體內的滑動視窗限流:同一個 key 在 window 期間內最多允許 maxEvents 次。
 * 單機部署足夠;多台機器時應改用 Redis 等共用儲存。
 */
public class SlidingWindowRateLimiter {

    /** 追蹤的 key 超過這個數量時,先清掉已過期的紀錄,避免記憶體無限成長 */
    private static final int PURGE_THRESHOLD = 10_000;

    private final int maxEvents;
    private final Duration window;
    private final Clock clock;
    private final Map<String, Deque<Instant>> events = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxEvents, Duration window, Clock clock) {
        this.maxEvents = maxEvents;
        this.window = window;
        this.clock = clock;
    }

    /** 未超過上限則記錄一次並回傳 true;超過上限回傳 false(不記錄) */
    public boolean tryAcquire(String key) {
        if (events.size() > PURGE_THRESHOLD) {
            purgeExpired();
        }
        Instant now = clock.instant();
        Deque<Instant> timestamps = events.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            dropExpired(timestamps, now);
            if (timestamps.size() >= maxEvents) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private void dropExpired(Deque<Instant> timestamps, Instant now) {
        Instant cutoff = now.minus(window);
        while (!timestamps.isEmpty() && !timestamps.peekFirst().isAfter(cutoff)) {
            timestamps.pollFirst();
        }
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        events.entrySet().removeIf(entry -> {
            synchronized (entry.getValue()) {
                dropExpired(entry.getValue(), now);
                return entry.getValue().isEmpty();
            }
        });
    }
}
