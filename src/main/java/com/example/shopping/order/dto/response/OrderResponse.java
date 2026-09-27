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
    private int pointsUsed;
    private BigDecimal shippingFee;
    private int pointsEarned;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String buyerNote;
    private String shippingCarrier;
    private String trackingNumber;
    private LocalDateTime shippedAt;
    private LocalDateTime paymentDeadline;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private List<OrderStatusLogResponse> statusLogs;
    /** 可申請退貨的期限,只有已完成的訂單才有值 */
    private LocalDateTime returnDeadline;
    private ReturnSummaryResponse returnRequest;

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
                order.getPointsUsed(),
                order.getShippingFee(),
                order.getPointsEarned(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getReceiverAddress(),
                order.getBuyerNote(),
                order.getShippingCarrier(),
                order.getTrackingNumber(),
                order.getShippedAt(),
                order.getPaymentDeadline(),
                order.getCreatedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getStatusLogs().stream().map(OrderStatusLogResponse::from).toList(),
                order.getReturnDeadline(),
                ReturnSummaryResponse.from(order.getReturnRequest()));
    }
}
