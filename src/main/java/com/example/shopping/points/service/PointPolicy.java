package com.example.shopping.points.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 購物金規則:1 點 = NT$1。訂單完成依實付金額回饋 earnRate(無條件捨去);
 * 結帳時最多折抵「套用優惠券後應付金額 × maxRedeemRatio」(無條件捨去)。
 */
@Component
public class PointPolicy {

    private final BigDecimal earnRate;
    private final BigDecimal maxRedeemRatio;

    public PointPolicy(@Value("${app.points.earn-rate:0.01}") BigDecimal earnRate,
                       @Value("${app.points.max-redeem-ratio:0.5}") BigDecimal maxRedeemRatio) {
        this.earnRate = earnRate;
        this.maxRedeemRatio = maxRedeemRatio;
    }

    public int pointsEarnedFor(BigDecimal paidAmount) {
        return pointsEarnedFor(paidAmount, BigDecimal.ONE);
    }

    /** 依會員等級倍率計算回饋點數(無條件捨去) */
    public int pointsEarnedFor(BigDecimal paidAmount, BigDecimal multiplier) {
        return paidAmount.multiply(earnRate).multiply(multiplier).setScale(0, RoundingMode.DOWN).intValue();
    }

    public int maxRedeemable(int balance, BigDecimal payableAmount) {
        int cap = payableAmount.multiply(maxRedeemRatio).setScale(0, RoundingMode.DOWN).intValue();
        return Math.max(0, Math.min(balance, cap));
    }

    public BigDecimal getEarnRate() {
        return earnRate;
    }

    public BigDecimal getMaxRedeemRatio() {
        return maxRedeemRatio;
    }
}
