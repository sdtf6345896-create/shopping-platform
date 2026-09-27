package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Orders> hasMemberId(Long memberId) {
        return (root, query, cb) -> memberId == null ? null : cb.equal(root.get("member").get("id"), memberId);
    }

    public static Specification<Orders> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    /** 訂單編號、收件人、收件電話或會員 Email 包含關鍵字 */
    public static Specification<Orders> keywordMatches(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("orderNo")), pattern),
                    cb.like(cb.lower(root.get("receiverName")), pattern),
                    cb.like(root.get("receiverPhone"), pattern),
                    cb.like(cb.lower(root.join("member").get("email")), pattern));
        };
    }

    /** 建立日期介於 startDate ~ endDate(含頭尾),任一端可為 null */
    public static Specification<Orders> createdBetween(LocalDate startDate, LocalDate endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) {
                return null;
            }
            if (startDate == null) {
                return cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay());
            }
            if (endDate == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay());
            }
            return cb.and(
                    cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()),
                    cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
        };
    }
}
