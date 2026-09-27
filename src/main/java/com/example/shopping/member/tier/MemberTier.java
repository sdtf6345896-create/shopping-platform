package com.example.shopping.member.tier;

import java.math.BigDecimal;

/**
 * 會員等級:依近 12 個月已完成訂單的商品消費金額(不含運費)決定,等級越高購物金回饋倍率越高。
 */
public enum MemberTier {

    NORMAL("一般會員", new BigDecimal("0"), new BigDecimal("1")),
    SILVER("銀卡會員", new BigDecimal("5000"), new BigDecimal("1.5")),
    GOLD("金卡會員", new BigDecimal("20000"), new BigDecimal("2"));

    private final String label;
    private final BigDecimal threshold;
    private final BigDecimal pointsMultiplier;

    MemberTier(String label, BigDecimal threshold, BigDecimal pointsMultiplier) {
        this.label = label;
        this.threshold = threshold;
        this.pointsMultiplier = pointsMultiplier;
    }

    public static MemberTier forSpending(BigDecimal spending) {
        MemberTier result = NORMAL;
        for (MemberTier tier : values()) {
            if (spending.compareTo(tier.threshold) >= 0) {
                result = tier;
            }
        }
        return result;
    }

    /** 下一個等級,已是最高級回傳 null */
    public MemberTier next() {
        int index = ordinal() + 1;
        return index < values().length ? values()[index] : null;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getThreshold() {
        return threshold;
    }

    public BigDecimal getPointsMultiplier() {
        return pointsMultiplier;
    }
}
