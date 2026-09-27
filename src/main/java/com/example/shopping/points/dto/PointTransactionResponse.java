package com.example.shopping.points.dto;

import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.points.entity.PointTransaction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PointTransactionResponse {

    private Long id;
    private Long orderId;
    private int amount;
    private PointTransactionType type;
    private String description;
    private int balanceAfter;
    private LocalDateTime createdAt;

    public static PointTransactionResponse from(PointTransaction tx) {
        return new PointTransactionResponse(tx.getId(), tx.getOrderId(), tx.getAmount(), tx.getType(),
                tx.getDescription(), tx.getBalanceAfter(), tx.getCreatedAt());
    }
}
