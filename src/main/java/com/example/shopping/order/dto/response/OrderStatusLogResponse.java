package com.example.shopping.order.dto.response;

import com.example.shopping.common.enums.OrderActor;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.OrderStatusLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderStatusLogResponse {

    private OrderStatus fromStatus;
    private OrderStatus toStatus;
    private OrderActor actor;
    private String note;
    private LocalDateTime createdAt;

    public static OrderStatusLogResponse from(OrderStatusLog log) {
        return new OrderStatusLogResponse(
                log.getFromStatus(), log.getToStatus(), log.getActor(), log.getNote(), log.getCreatedAt());
    }
}
