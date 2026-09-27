package com.example.shopping.coupon.service;

/**
 * 查詢會員用過某張優惠券幾次。使用紀錄在訂單模組,由訂單模組實作,
 * 優惠券模組不必依賴訂單的 repository(避免兩個模組互相依賴)。
 */
public interface CouponUsageLookup {

    /** 不含已取消的訂單 */
    long countUsedBy(Long memberId, Long couponId);
}
