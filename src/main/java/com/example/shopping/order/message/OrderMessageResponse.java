package com.example.shopping.order.message;

import java.time.LocalDateTime;

public record OrderMessageResponse(Long id, MessageSender sender, String content, LocalDateTime createdAt) {

    public static OrderMessageResponse from(OrderMessage message) {
        return new OrderMessageResponse(message.getId(), message.getSender(), message.getContent(),
                message.getCreatedAt());
    }
}
