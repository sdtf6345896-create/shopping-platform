package com.example.shopping.security;

import com.example.shopping.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防暴力破解:同一帳號連續登入失敗達上限後,鎖定一段時間拒絕登入(含密碼正確的嘗試)。
 * <p>
 * 狀態存在記憶體,單機部署足夠;不論帳號是否存在都會計數,避免藉由錯誤訊息差異探測帳號。
 */
@Component
public class LoginAttemptService {

    /** 記憶體中最多保留的帳號數,超過時先清掉已過期的紀錄 */
    private static final int MAX_TRACKED_ACCOUNTS = 10_000;

    private final int maxAttempts;
    private final Duration lockDuration;
    private final Clock clock;
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    @Autowired
    public LoginAttemptService(@Value("${app.security.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this(maxAttempts, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxAttempts, Duration lockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    /** 帳號被鎖定時丟出 429 */
    public void checkNotLocked(String scope, String account) {
        Attempt attempt = attempts.get(key(scope, account));
        if (attempt == null || attempt.lockedUntil == null) {
            return;
        }
        Instant now = clock.instant();
        if (now.isBefore(attempt.lockedUntil)) {
            long minutes = (Duration.between(now, attempt.lockedUntil).toSeconds() + 59) / 60;
            throw new BusinessException(
                    "登入失敗次數過多,帳號已暫時鎖定,請於 " + minutes + " 分鐘後再試", HttpStatus.TOO_MANY_REQUESTS);
        }
        attempts.remove(key(scope, account), attempt);
    }

    public void recordFailure(String scope, String account) {
        if (attempts.size() >= MAX_TRACKED_ACCOUNTS) {
            purgeExpired();
        }
        Instant now = clock.instant();
        attempts.compute(key(scope, account), (k, existing) -> {
            Attempt attempt = existing == null || isExpired(existing, now) ? new Attempt() : existing;
            attempt.failures++;
            attempt.lastFailureAt = now;
            if (attempt.failures >= maxAttempts) {
                attempt.lockedUntil = now.plus(lockDuration);
            }
            return attempt;
        });
    }

    public void recordSuccess(String scope, String account) {
        attempts.remove(key(scope, account));
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        attempts.values().removeIf(attempt -> isExpired(attempt, now));
    }

    private static String key(String scope, String account) {
        return scope + ":" + (account == null ? "" : account.trim().toLowerCase(Locale.ROOT));
    }

    /** 鎖定期已過,或最後一次失敗距今超過一個鎖定期,都視為過期、重新計數 */
    private boolean isExpired(Attempt attempt, Instant now) {
        Instant expiresAt = attempt.lockedUntil != null
                ? attempt.lockedUntil
                : attempt.lastFailureAt.plus(lockDuration);
        return !now.isBefore(expiresAt);
    }

    private static final class Attempt {
        private int failures;
        private Instant lastFailureAt;
        private Instant lockedUntil;
    }
}
