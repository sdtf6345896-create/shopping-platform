package com.example.shopping.returns.dto.response;

import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.returns.entity.ReturnRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReturnRequestResponse {

    private Long id;
    private Long orderId;
    private String orderNo;
    private BigDecimal orderTotalAmount;
    private String memberEmail;
    private String receiverName;
    private String reason;
    private ReturnStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public static ReturnRequestResponse from(ReturnRequest request) {
        Orders order = request.getOrder();
        return new ReturnRequestResponse(
                request.getId(),
                order.getId(),
                order.getOrderNo(),
                order.getTotalAmount(),
                order.getMember().getEmail(),
                order.getReceiverName(),
                request.getReason(),
                request.getStatus(),
                request.getAdminNote(),
                request.getCreatedAt(),
                request.getProcessedAt());
    }
}
