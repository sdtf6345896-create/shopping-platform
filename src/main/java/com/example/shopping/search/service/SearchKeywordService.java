package com.example.shopping.search.service;

import com.example.shopping.search.entity.SearchKeyword;
import com.example.shopping.search.repository.SearchKeywordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** 熱門搜尋:記錄與查詢。記錄失敗只寫 log,絕不影響搜尋本身 */
@Service
public class SearchKeywordService {

    private static final Logger log = LoggerFactory.getLogger(SearchKeywordService.class);
    static final int MAX_KEYWORD_LENGTH = 30;
    static final int HOT_WINDOW_DAYS = 30;

    private final SearchKeywordRepository repository;
    private final TransactionTemplate requiresNew;

    public SearchKeywordService(SearchKeywordRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** 正規化:去頭尾空白、連續空白變一個、轉小寫;太長或空白回傳 null(不記錄) */
    static String normalize(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        return normalized.isEmpty() || normalized.length() > MAX_KEYWORD_LENGTH ? null : normalized;
    }

    /**
     * 記錄一次搜尋。在獨立交易中執行,且 try/catch 包在交易「外面」:
     * 若在交易裡面接住例外(例如兩個請求同時插入同一個新關鍵字撞到 unique),交易已被標記 rollback-only,
     * 方法結束時會丟 UnexpectedRollbackException,反而讓搜尋 API 失敗。
     */
    public void record(String rawKeyword) {
        String keyword = normalize(rawKeyword);
        if (keyword == null) {
            return;
        }
        try {
            requiresNew.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                if (repository.increment(keyword, now) == 0) {
                    SearchKeyword entry = new SearchKeyword();
                    entry.setKeyword(keyword);
                    entry.setSearchCount(1);
                    entry.setLastSearchedAt(now);
                    repository.saveAndFlush(entry);
                }
            });
        } catch (DataIntegrityViolationException e) {
            // 兩個請求同時第一次搜尋同一個字:另一邊已經插入,這次的計數就算了
            log.debug("熱門搜尋同時插入:{}", keyword);
        } catch (RuntimeException e) {
            log.warn("記錄熱門搜尋失敗:{} {}", keyword, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<String> hot(int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        return repository.findHot(LocalDateTime.now().minusDays(HOT_WINDOW_DAYS), PageRequest.of(0, size)).stream()
                .map(SearchKeyword::getKeyword)
                .toList();
    }
}
