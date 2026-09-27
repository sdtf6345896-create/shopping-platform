package com.example.shopping.coupon.controller;

import com.example.shopping.cart.service.CartService;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.coupon.dto.request.CouponApplyRequest;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.dto.response.WalletCouponResponse;
import com.example.shopping.coupon.service.CouponService;
import com.example.shopping.coupon.service.CouponWalletService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;
    private final CouponWalletService couponWalletService;

    public CouponController(CouponService couponService, CartService cartService,
                            CouponWalletService couponWalletService) {
        this.couponService = couponService;
        this.cartService = cartService;
        this.couponWalletService = couponWalletService;
    }

    @GetMapping("/center")
    public ApiResponse<List<WalletCouponResponse>> center() {
        return ApiResponse.success(couponWalletService.listClaimable(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/{id}/claim")
    public ApiResponse<WalletCouponResponse> claim(@PathVariable Long id) {
        return ApiResponse.success("領取成功", couponWalletService.claim(SecurityUtils.getCurrentUserId(), id));
    }

    @GetMapping("/mine")
    public ApiResponse<List<WalletCouponResponse>> mine() {
        return ApiResponse.success(couponWalletService.listMine(SecurityUtils.getCurrentUserId()));
    }

    /**
     * 依目前購物車選取的項目試算優惠券折扣,不會消耗使用名額。
     */
    @PostMapping("/apply")
    public ApiResponse<CouponApplyResponse> apply(@Valid @RequestBody CouponApplyRequest request) {
        Long memberId = SecurityUtils.getCurrentUserId();
        BigDecimal subtotal = cartService.calculateSubtotal(memberId, request.getCartItemIds());
        return ApiResponse.success(couponService.preview(request.getCode(), subtotal, memberId));
    }
}
