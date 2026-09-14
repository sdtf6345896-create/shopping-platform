package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Orders> hasMemberId(Long memberId) {
        return (root, query, cb) -> memberId == null ? null : cb.equal(root.get("member").get("id"), memberId);
    }

    public static Specification<Orders> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
