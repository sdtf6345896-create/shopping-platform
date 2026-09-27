package com.example.shopping.order.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.coupon.service.CouponUsageLookup;
import com.example.shopping.order.repository.OrderRepository;
import org.springframework.stereotype.Component;

@Component
public class OrderCouponUsageLookup implements CouponUsageLookup {

    private final OrderRepository orderRepository;

    public OrderCouponUsageLookup(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public long countUsedBy(Long memberId, Long couponId) {
        return orderRepository.countByMemberIdAndCouponIdAndStatusNot(memberId, couponId, OrderStatus.CANCELLED);
    }
}
