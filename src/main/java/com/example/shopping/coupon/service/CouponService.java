package com.example.shopping.coupon.service;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.coupon.dto.request.CouponRequest;
import com.example.shopping.coupon.dto.request.CouponStatusRequest;
import com.example.shopping.coupon.dto.response.CouponApplyResponse;
import com.example.shopping.coupon.dto.response.CouponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface CouponService {

    Page<CouponResponse> listAdmin(String keyword, CouponStatus status, Pageable pageable);

    CouponResponse getAdmin(Long id);

    CouponResponse create(CouponRequest request);

    CouponResponse update(Long id, CouponRequest request);

    CouponResponse updateStatus(Long id, CouponStatusRequest request);

    void delete(Long id);

    /**
     * 試算優惠券折抵金額,不會消耗使用張數;供購物車/結帳頁預覽用。
     */
    CouponApplyResponse preview(String code, BigDecimal subtotal);

    /**
     * 結帳時實際套用優惠券,驗證通過後會佔用一張使用名額。
     */
    CouponApplyResponse reserve(String code, BigDecimal subtotal);

    /**
     * 訂單取消時歸還優惠券使用名額。
     */
    void release(Long couponId);
}
