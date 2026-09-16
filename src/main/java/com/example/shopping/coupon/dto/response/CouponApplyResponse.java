package com.example.shopping.coupon.dto.response;

import com.example.shopping.common.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CouponApplyResponse {

    private Long couponId;
    private String code;
    private String name;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
}
