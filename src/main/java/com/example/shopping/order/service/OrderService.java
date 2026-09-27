package com.example.shopping.order.service;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface OrderService {

    Page<OrderResponse> listMyOrders(Long memberId, OrderStatus status, Pageable pageable);

    OrderResponse getMyOrder(Long memberId, Long orderId);

    OrderResponse checkout(Long memberId, CheckoutRequest request);

    OrderResponse pay(Long memberId, Long orderId);

    OrderResponse cancelByMember(Long memberId, Long orderId);

    Page<OrderResponse> listAdmin(OrderStatus status, Pageable pageable);

    OrderResponse getAdminOrder(Long orderId);

    OrderResponse updateStatus(Long orderId, OrderStatusRequest request);

    /**
     * 取消所有付款期限早於 now 的待付款訂單。
     *
     * @return 取消的筆數
     */
    int cancelExpiredOrders(LocalDateTime now);
}
