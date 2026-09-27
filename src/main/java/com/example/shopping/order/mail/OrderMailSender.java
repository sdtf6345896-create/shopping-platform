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

    public void notifyReturnRejected(Orders order, String note) {
        String body = String.format("您好 %s,%n%n訂單編號:%s%n%n很抱歉,您的退貨申請未通過審核。",
                order.getReceiverName(), order.getOrderNo());
        if (note != null) {
            body += System.lineSeparator() + "說明:" + note;
        }
        mailSender.send(order.getMember().getEmail(), "退貨申請結果通知", body);
    }

    private String subjectFor(OrderStatus status) {
        return switch (status) {
            case PENDING_PAYMENT -> "訂單成立通知";
            case PAID -> "付款成功通知";
            case SHIPPING -> "商品出貨通知";
            case COMPLETED -> "訂單完成通知";
            case CANCELLED -> "訂單取消通知";
            case REFUNDED -> "退貨退款完成通知";
        };
    }

    private String messageFor(Orders order) {
        return switch (order.getStatus()) {
            case PENDING_PAYMENT -> "您的訂單已成立,請於期限內完成付款。";
            case PAID -> "您的訂單已付款成功,我們將盡快為您出貨。";
            case SHIPPING -> "您的訂單已出貨,請留意收件:" + order.getReceiverAddress()
                    + (order.getTrackingNumber() != null
                    ? String.format("%n物流業者:%s%n物流單號:%s", order.getShippingCarrier(), order.getTrackingNumber())
                    : "");
            case COMPLETED -> "您的訂單已完成,感謝您的購買。";
            case CANCELLED -> "您的訂單已取消。";
            case REFUNDED -> "您的退貨申請已核准,款項將退回原付款方式。";
        };
    }

    private String bodyFor(Orders order) {
        return String.format("您好 %s,%n%n訂單編號:%s%n訂單金額:NT$ %s%n%n%s",
                order.getReceiverName(), order.getOrderNo(), order.getTotalAmount(), messageFor(order));
    }
}
