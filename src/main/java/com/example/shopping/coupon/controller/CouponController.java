package com.example.shopping.coupon.controller;

import com.example.shopping.cart.service.CartService;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.coupon.dto.request.CouponApplyRequest;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;

    public CouponController(CouponService couponService, CartService cartService) {
        this.couponService = couponService;
        this.cartService = cartService;
    }

    /**
     * 依目前購物車選取的項目試算優惠券折扣,不會消耗使用名額。
     */
    @PostMapping("/apply")
    public ApiResponse<CouponApplyResponse> apply(@Valid @RequestBody CouponApplyRequest request) {
        Long memberId = SecurityUtils.getCurrentUserId();
        BigDecimal subtotal = cartService.calculateSubtotal(memberId, request.getCartItemIds());
        return ApiResponse.success(couponService.preview(request.getCode(), subtotal));
    }
}
