package com.example.shopping.order.mail;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.order.entity.Orders;
import org.springframework.stereotype.Component;

@Component
public class OrderMailSender {

    private final MailSender mailSender;

    public OrderMailSender(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void notifyStatusChanged(Orders order) {
        String toEmail = order.getMember().getEmail();
        mailSender.send(toEmail, subjectFor(order.getStatus()), bodyFor(order));
    }

    private String subjectFor(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "訂單成立通知";
            case PAID -> "付款成功通知";
            case SHIPPING -> "商品出貨通知";
            case COMPLETED -> "訂單完成通知";
            case CANCELLED -> "訂單取消通知";
        };
    }

    private String messageFor(Orders order) {
        return switch (order.getStatus()) {
            case PENDING_PAYMENT -> "您的訂單已成立,請於期限內完成付款。";
            case PAID -> "您的訂單已付款成功,我們將盡快為您出貨。";
            case SHIPPING -> "您的訂單已出貨,請留意收件:" + order.getReceiverAddress();
            case COMPLETED -> "您的訂單已完成,感謝您的購買。";
            case CANCELLED -> "您的訂單已取消。";
        };
    }

    private String bodyFor(Orders order) {
        return String.format("您好 %s,%n%n訂單編號:%s%n訂單金額:NT$ %s%n%n%s",
                order.getReceiverName(), order.getOrderNo(), order.getTotalAmount(), messageFor(order));
    }
}
