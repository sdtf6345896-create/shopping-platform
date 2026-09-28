package com.example.shopping.promotion;

import java.time.LocalDateTime;

/** @param running 目前是否進行中(啟用且在活動期間內) */
public record PromotionResponse(Long id, String name, Long categoryId, String categoryName, int minQuantity,
                                int discountPercent, LocalDateTime startAt, LocalDateTime endAt, boolean active,
                                boolean running) {

    public static PromotionResponse from(Promotion promotion, LocalDateTime now) {
        return new PromotionResponse(promotion.getId(), promotion.getName(),
                promotion.getCategory() == null ? null : promotion.getCategory().getId(),
                promotion.getCategory() == null ? null : promotion.getCategory().getName(),
                promotion.getMinQuantity(), promotion.getDiscountPercent(), promotion.getStartAt(),
                promotion.getEndAt(), promotion.isActive(), promotion.isRunningAt(now));
    }
}
