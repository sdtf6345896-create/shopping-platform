package com.example.shopping.coupon.dto.response;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CouponResponse {

    private Long id;
    private String code;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minSpendAmount;
    private Integer totalQuantity;
    private int usedQuantity;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private CouponStatus status;
    private LocalDateTime createdAt;

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getId(),
                coupon.getCode(),
                coupon.getName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinSpendAmount(),
                coupon.getTotalQuantity(),
                coupon.getUsedQuantity(),
                coupon.getStartAt(),
                coupon.getEndAt(),
                coupon.getStatus(),
                coupon.getCreatedAt());
    }
}
