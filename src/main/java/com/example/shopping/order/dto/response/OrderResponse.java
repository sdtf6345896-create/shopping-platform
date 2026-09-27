package com.example.shopping.order.dto.response;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.order.entity.Orders;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class OrderResponse {

    private Long id;
    private String orderNo;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponCode;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String shippingCarrier;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private List<OrderStatusLogResponse> statusLogs;

    public static OrderResponse from(Orders order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCouponCode(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getShippingCarrier(),
                order.getTrackingNumber(),
                order.getShippedAt(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getStatusLogs().stream().map(OrderStatusLogResponse::from).toList());
    }
}
