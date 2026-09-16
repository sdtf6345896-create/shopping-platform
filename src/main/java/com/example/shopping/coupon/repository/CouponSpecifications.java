package com.example.shopping.coupon.repository;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.coupon.entity.Coupon;
import org.springframework.data.jpa.domain.Specification;

public final class CouponSpecifications {

    private CouponSpecifications() {
    }

    public static Specification<Coupon> hasStatus(CouponStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Coupon> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("code")), pattern),
                    cb.like(cb.lower(root.get("name")), pattern));
        };
    }
}
