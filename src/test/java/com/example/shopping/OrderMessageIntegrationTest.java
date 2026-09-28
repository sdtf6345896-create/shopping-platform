package com.example.shopping;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.message.AwaitingMessageResponse;
import com.example.shopping.order.message.MessageSender;
import com.example.shopping.order.message.OrderMessageResponse;
import com.example.shopping.order.message.OrderMessageService;
import com.example.shopping.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/** 會員只能看/留自己訂單的言;會員留言後列入待回覆,賣家回覆後移出並通知會員 */
@SpringBootTest
@ActiveProfiles("test")
class OrderMessageIntegrationTest {

    @Autowired
    private OrderMessageService messageService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Member member(String name) {
        Member member = new Member();
        member.setEmail(name + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword("x");
        member.setName(name);
        member.setEmailVerified(true);
        return memberRepository.save(member);
    }

    private Orders order(Member member) {
        Orders order = new Orders();
        order.setOrderNo("M" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setMember(member);
        order.setSubtotalAmount(new BigDecimal("500"));
        order.setTotalAmount(new BigDecimal("560"));
        order.setShippingFee(new BigDecimal("60"));
        order.setPaymentMethod(PaymentMethod.ATM);
        order.setStatus(OrderStatus.PAID);
        order.setReceiverName(member.getName());
        order.setReceiverPhone("0912345678");
        order.setReceiverAddress("台北市");
        return orderRepository.save(order);
    }

    private List<Long> awaitingOrderIds() {
        return messageService.listAwaitingReply(PageRequest.of(0, 500)).getContent().stream()
                .map(AwaitingMessageResponse::orderId).toList();
    }

    @Test
    void conversationFlow() {
        Member buyer = member("buyer");
        Member stranger = member("stranger");
        Orders order = order(buyer);

        messageService.postByMember(buyer.getId(), order.getId(), "  可以改成週六送貨嗎? ");
        messageService.postByMember(buyer.getId(), order.getId(), "下午比較方便");
        assertThat(awaitingOrderIds()).contains(order.getId());

        messageService.replyByAdmin(order.getId(), "沒問題,已為您備註週六下午配送");
        assertThat(awaitingOrderIds()).doesNotContain(order.getId());
        assertThat(notificationRepository.findByMemberId(buyer.getId(), PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);

        assertThat(messageService.listForMember(buyer.getId(), order.getId()))
                .extracting(OrderMessageResponse::sender, OrderMessageResponse::content)
                .containsExactly(
                        tuple(MessageSender.MEMBER, "可以改成週六送貨嗎?"),
                        tuple(MessageSender.MEMBER, "下午比較方便"),
                        tuple(MessageSender.ADMIN, "沒問題,已為您備註週六下午配送"));

        // 會員再追問 → 又回到待回覆
        messageService.postByMember(buyer.getId(), order.getId(), "謝謝!");
        assertThat(awaitingOrderIds()).contains(order.getId());

        // 別人的訂單看不到也留不了言
        assertThatThrownBy(() -> messageService.listForMember(stranger.getId(), order.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> messageService.postByMember(stranger.getId(), order.getId(), "hi"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
