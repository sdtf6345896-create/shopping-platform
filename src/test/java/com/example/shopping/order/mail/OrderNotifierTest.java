package com.example.shopping.order.mail;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.notification.mail.MailSender;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.order.entity.Orders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderNotifierTest {

    @Mock
    private MailSender mailSender;
    @Mock
    private NotificationService notificationService;

    private Orders order(OrderStatus status) {
        Member member = new Member();
        member.setId(3L);
        member.setEmail("member@example.com");

        Orders order = new Orders();
        order.setId(9L);
        order.setMember(member);
        order.setStatus(status);
        order.setOrderNo("ORD20260927000001");
        order.setReceiverName("王小明");
        order.setReceiverAddress("台北市大安區復興南路一段1號");
        order.setTotalAmount(new BigDecimal("1180.00"));
        return order;
    }

    @Test
    void notifyStatusChanged_sendsPlacedNotification() {
        new OrderNotifier(mailSender, notificationService).notifyStatusChanged(order(OrderStatus.PENDING_PAYMENT));

        verify(mailSender).send(eq("member@example.com"), eq("訂單成立通知"),
                argThat(body -> body.contains("ORD20260927000001") && body.contains("成立")));
    }

    @Test
    void notifyStatusChanged_sendsShippingNotification_withAddress() {
        new OrderNotifier(mailSender, notificationService).notifyStatusChanged(order(OrderStatus.SHIPPING));

        verify(mailSender).send(eq("member@example.com"), eq("商品出貨通知"),
                argThat(body -> body.contains("台北市大安區復興南路一段1號")));
    }

    @Test
    void notifyStatusChanged_includesTrackingInfo_whenShipped() {
        Orders order = order(OrderStatus.SHIPPING);
        order.setShippingCarrier("黑貓宅急便");
        order.setTrackingNumber("TRK123456");

        new OrderNotifier(mailSender, notificationService).notifyStatusChanged(order);

        verify(mailSender).send(eq("member@example.com"), eq("商品出貨通知"),
                argThat(body -> body.contains("黑貓宅急便") && body.contains("TRK123456")));
    }

    @Test
    void notifyStatusChanged_sendsCancelledNotification() {
        new OrderNotifier(mailSender, notificationService).notifyStatusChanged(order(OrderStatus.CANCELLED));

        verify(mailSender).send(eq("member@example.com"), eq("訂單取消通知"), argThat(body -> body.contains("取消")));
    }

    @Test
    void notifyStatusChanged_alsoCreatesInboxNotificationLinkingToOrder() {
        new OrderNotifier(mailSender, notificationService).notifyStatusChanged(order(OrderStatus.SHIPPING));

        verify(notificationService).notify(eq(3L), eq(NotificationType.ORDER), eq("商品出貨通知"),
                argThat(content -> content.contains("ORD20260927000001")), eq("/orders/9"));
    }

    @Test
    void notifyReturnRejected_createsReturnNotificationWithReason() {
        new OrderNotifier(mailSender, notificationService).notifyReturnRejected(order(OrderStatus.COMPLETED), "已拆封");

        verify(notificationService).notify(eq(3L), eq(NotificationType.RETURN), eq("退貨申請未通過"),
                argThat(content -> content.contains("已拆封")), eq("/orders/9"));
    }
}
