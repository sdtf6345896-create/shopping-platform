package com.example.shopping.order.message;

import java.time.LocalDateTime;

/** 後台「待回覆留言」列表:每筆是一張訂單與會員最後一則留言 */
public record AwaitingMessageResponse(Long orderId, String orderNo, String memberName, String memberEmail,
                                      String lastMessage, LocalDateTime lastMessageAt) {

    public static AwaitingMessageResponse from(OrderMessage message) {
        return new AwaitingMessageResponse(message.getOrder().getId(), message.getOrder().getOrderNo(),
                message.getOrder().getMember().getName(), message.getOrder().getMember().getEmail(),
                message.getContent(), message.getCreatedAt());
    }
}
