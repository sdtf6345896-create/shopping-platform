package com.example.shopping.recommendation.service;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.browsinghistory.repository.BrowsingHistoryItemRepository;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.dto.response.ProductListResponse;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.repository.ProductRepository;
import com.example.shopping.recommendation.dto.RecommendationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 「為你推薦」:依會員最近瀏覽的商品找出最常看的分類,推薦這些分類中還沒看過的熱銷商品,
 * 不足時用全站熱銷補齊。規則簡單、可解釋,不需要額外的推薦系統。
 */
@Service
@Transactional(readOnly = true)
public class RecommendationService {

    /** 只參考最近瀏覽的這幾筆,反映當下興趣 */
    static final int RECENT_HISTORY_SIZE = 20;
    /** 最多取幾個偏好分類 */
    static final int TOP_CATEGORIES = 3;

    private final BrowsingHistoryItemRepository historyRepository;
    private final ProductRepository productRepository;

    public RecommendationService(BrowsingHistoryItemRepository historyRepository,
                                 ProductRepository productRepository) {
        this.historyRepository = historyRepository;
        this.productRepository = productRepository;
    }

    public RecommendationResponse recommend(Long memberId, int limit) {
        int size = Math.min(Math.max(limit, 1), 20);
        List<BrowsingHistoryItem> history = historyRepository.findAllByMemberIdWithDetails(memberId,
                PageRequest.of(0, RECENT_HISTORY_SIZE, Sort.by(Sort.Direction.DESC, "viewedAt"))).getContent();

        Set<Long> exclude = new HashSet<>();
        // 以出現次數排序分類;次數相同時,較近期瀏覽的在前(LinkedHashMap 保留第一次出現的順序)
        Map<Long, Integer> categoryCounts = new LinkedHashMap<>();
        for (BrowsingHistoryItem item : history) {
            Product viewed = item.getProduct();
            exclude.add(viewed.getId());
            categoryCounts.merge(viewed.getCategory().getId(), 1, Integer::sum);
        }
        List<Long> topCategories = categoryCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(TOP_CATEGORIES)
                .map(Map.Entry::getKey)
                .toList();

        List<Product> picks = new ArrayList<>();
        if (!topCategories.isEmpty()) {
            picks.addAll(productRepository.findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
                    topCategories, ProductStatus.ON_SALE, withSentinel(exclude), PageRequest.of(0, size)));
        }
        if (picks.size() < size) {
            picks.forEach(p -> exclude.add(p.getId()));
            picks.addAll(productRepository.findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
                    ProductStatus.ON_SALE, withSentinel(exclude), PageRequest.of(0, size - picks.size())));
        }

        return new RecommendationResponse(!history.isEmpty(),
                picks.stream().map(ProductListResponse::from).toList());
    }

    /** NOT IN 空集合在部分資料庫是語法錯誤,放一個不存在的 id 墊著 */
    private static Set<Long> withSentinel(Set<Long> ids) {
        if (!ids.isEmpty()) {
            return ids;
        }
        return Set.of(-1L);
    }
}
