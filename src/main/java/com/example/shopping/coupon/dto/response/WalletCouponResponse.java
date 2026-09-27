package com.example.shopping.coupon.dto.response;

import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 領券中心 / 我的優惠券顯示用(不含後台用的發放數量等資訊) */
public record WalletCouponResponse(Long id, String code, String name, DiscountType discountType,
                                   BigDecimal discountValue, BigDecimal maxDiscountAmount,
                                   BigDecimal minSpendAmount, LocalDateTime endAt, boolean claimed) {

    public static WalletCouponResponse from(Coupon coupon, boolean claimed) {
        return new WalletCouponResponse(coupon.getId(), coupon.getCode(), coupon.getName(), coupon.getDiscountType(),
                coupon.getDiscountValue(), coupon.getMaxDiscountAmount(), coupon.getMinSpendAmount(),
                coupon.getEndAt(), claimed);
    }
}
