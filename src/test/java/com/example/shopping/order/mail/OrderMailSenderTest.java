package com.example.shopping.order.mail;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.member.entity.Member;
import com.example.shopping.notification.mail.MailSender;
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
class OrderMailSenderTest {

    @Mock
    private MailSender mailSender;

    private Orders order(OrderStatus status) {
        Member member = new Member();
        member.setEmail("member@example.com");

        Orders order = new Orders();
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
        new OrderMailSender(mailSender).notifyStatusChanged(order(OrderStatus.PENDING_PAYMENT));

        verify(mailSender).send(eq("member@example.com"), eq("訂單成立通知"),
                argThat(body -> body.contains("ORD20260927000001") && body.contains("成立")));
    }

    @Test
    void notifyStatusChanged_sendsShippingNotification_withAddress() {
        new OrderMailSender(mailSender).notifyStatusChanged(order(OrderStatus.SHIPPING));

        verify(mailSender).send(eq("member@example.com"), eq("商品出貨通知"),
                argThat(body -> body.contains("台北市大安區復興南路一段1號")));
    }

    @Test
    void notifyStatusChanged_sendsCancelledNotification() {
        new OrderMailSender(mailSender).notifyStatusChanged(order(OrderStatus.CANCELLED));

        verify(mailSender).send(eq("member@example.com"), eq("訂單取消通知"), argThat(body -> body.contains("取消")));
    }
}
