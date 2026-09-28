package com.example.shopping.promotion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 滿件折扣試算(純計算,不碰資料庫)。一張訂單只套用折扣金額最大的一個活動;
 * 另外列出「差幾件就能享有」的活動,給購物車 / 結帳頁提示用。
 */
public final class PromotionCalculator {

    private PromotionCalculator() {
    }

    /**
     * @param categoryPath 商品所屬分類及其所有上層分類的 id(判斷是否落在活動分類底下)
     */
    public record Line(Set<Long> categoryPath, BigDecimal unitPrice, int quantity) {
    }

    /** 活動規則的最小資訊;categoryId 為 null 表示全站 */
    public record Rule(Long id, String name, Long categoryId, int minQuantity, int discountPercent) {
    }

    public record Hint(Long promotionId, String name, int missingQuantity, int discountPercent) {
    }

    public record Result(Long promotionId, String promotionName, BigDecimal discount, List<Hint> hints) {

        public static Result none() {
            return new Result(null, null, BigDecimal.ZERO, List.of());
        }

        public boolean applied() {
            return promotionId != null;
        }
    }

    public static Result evaluate(List<Rule> rules, List<Line> lines) {
        Rule best = null;
        BigDecimal bestDiscount = BigDecimal.ZERO;
        List<Hint> hints = new ArrayList<>();
        for (Rule rule : rules) {
            int quantity = 0;
            BigDecimal amount = BigDecimal.ZERO;
            for (Line line : lines) {
                if (rule.categoryId() == null || line.categoryPath().contains(rule.categoryId())) {
                    quantity += line.quantity();
                    amount = amount.add(line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())));
                }
            }
            if (quantity == 0) {
                continue;
            }
            if (quantity < rule.minQuantity()) {
                hints.add(new Hint(rule.id(), rule.name(), rule.minQuantity() - quantity, rule.discountPercent()));
                continue;
            }
            // 折扣無條件捨去到整數元
            BigDecimal discount = amount.multiply(BigDecimal.valueOf(rule.discountPercent()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            if (discount.compareTo(bestDiscount) > 0) {
                best = rule;
                bestDiscount = discount;
            }
        }
        hints.sort(Comparator.comparingInt(Hint::missingQuantity).thenComparing(Hint::discountPercent,
                Comparator.reverseOrder()));
        if (best == null) {
            return new Result(null, null, BigDecimal.ZERO, hints);
        }
        return new Result(best.id(), best.name(), bestDiscount, hints);
    }
}
