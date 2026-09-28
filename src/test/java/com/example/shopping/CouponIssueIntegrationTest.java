package com.example.shopping;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.DiscountType;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.coupon.dto.request.CouponIssueRequest;
import com.example.shopping.coupon.dto.response.CouponIssueResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.repository.MemberCouponRepository;
import com.example.shopping.coupon.service.CouponIssueService;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.tier.MemberTier;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 依等級 / Email 名單發券:只發給符合條件的啟用會員,已持有的略過,不重複通知 */
@SpringBootTest
@ActiveProfiles("test")
class CouponIssueIntegrationTest {

    @Autowired
    private CouponIssueService couponIssueService;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private final String suffix = UUID.randomUUID().toString().substring(0, 8);

    private Member member(String name, AccountStatus status) {
        Member member = new Member();
        member.setEmail(name + "-" + suffix + "@Example.com");
        member.setPassword("x");
        member.setName(name);
        member.setEmailVerified(true);
        member.setStatus(status);
        return memberRepository.save(member);
    }

    private void completedOrder(Member member, String merchandise) {
        Orders order = new Orders();
        order.setOrderNo("T" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        order.setMember(member);
        order.setSubtotalAmount(new BigDecimal(merchandise));
        order.setTotalAmount(new BigDecimal(merchandise).add(new BigDecimal("60")));
        order.setShippingFee(new BigDecimal("60"));
        order.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        order.setStatus(OrderStatus.COMPLETED);
        order.setReceiverName(member.getName());
        order.setReceiverPhone("0912345678");
        order.setReceiverAddress("台北市");
        orderRepository.save(order);
    }

    private Coupon coupon() {
        Coupon coupon = new Coupon();
        coupon.setCode("VIP" + suffix.toUpperCase());
        coupon.setName("VIP 專屬折價券");
        coupon.setDiscountType(DiscountType.FIXED_AMOUNT);
        coupon.setDiscountValue(new BigDecimal("200"));
        coupon.setEndAt(LocalDateTime.now().plusDays(30));
        return couponRepository.save(coupon);
    }

    private boolean holds(Member member, Coupon coupon) {
        return memberCouponRepository.existsByMemberIdAndCouponId(member.getId(), coupon.getId());
    }

    private long notificationsOf(Member member) {
        return notificationRepository.findByMemberId(member.getId(), PageRequest.of(0, 50)).getTotalElements();
    }

    @Test
    void issuesByTier_onlyToQualifyingActiveMembers_andSkipsHolders() {
        Member gold = member("gold", AccountStatus.ACTIVE);
        completedOrder(gold, "15000");
        completedOrder(gold, "6000");
        Member silver = member("silver", AccountStatus.ACTIVE);
        completedOrder(silver, "8000");
        Member disabledGold = member("disabled", AccountStatus.DISABLED);
        completedOrder(disabledGold, "30000");
        Coupon coupon = coupon();

        CouponIssueRequest request = new CouponIssueRequest();
        request.setTarget(CouponIssueRequest.Target.TIER);
        request.setMinTier(MemberTier.GOLD);
        CouponIssueResponse first = couponIssueService.issue(coupon.getId(), request);

        assertThat(holds(gold, coupon)).isTrue();
        assertThat(holds(silver, coupon)).isFalse();
        assertThat(holds(disabledGold, coupon)).isFalse();
        assertThat(notificationsOf(gold)).isEqualTo(1);
        assertThat(first.issued()).isEqualTo(first.targeted());

        // 再發一次:金卡會員已持有 → 略過且不重複通知;改成銀卡以上會多發給 silver
        request.setMinTier(MemberTier.SILVER);
        CouponIssueResponse second = couponIssueService.issue(coupon.getId(), request);
        assertThat(holds(silver, coupon)).isTrue();
        assertThat(notificationsOf(gold)).isEqualTo(1);
        assertThat(second.alreadyHeld()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void issuesByEmails_caseInsensitive_andReportsUnmatched() {
        Member alice = member("alice", AccountStatus.ACTIVE);
        Member disabled = member("gone", AccountStatus.DISABLED);
        Coupon coupon = coupon();

        CouponIssueRequest request = new CouponIssueRequest();
        request.setTarget(CouponIssueRequest.Target.EMAILS);
        request.setEmails(List.of(" ALICE-" + suffix + "@example.COM ", alice.getEmail(),
                disabled.getEmail(), "nobody@example.com", ""));
        CouponIssueResponse result = couponIssueService.issue(coupon.getId(), request);

        assertThat(result.targeted()).isEqualTo(1);
        assertThat(result.issued()).isEqualTo(1);
        assertThat(holds(alice, coupon)).isTrue();
        assertThat(result.unmatchedEmails())
                .containsExactly(disabled.getEmail().toLowerCase(), "nobody@example.com");
    }

    @Test
    void refusesExpiredOrDisabledCoupons() {
        Coupon coupon = coupon();
        coupon.setEndAt(LocalDateTime.now().minusMinutes(1));
        couponRepository.save(coupon);
        CouponIssueRequest request = new CouponIssueRequest();
        request.setTarget(CouponIssueRequest.Target.ALL);

        assertThatThrownBy(() -> couponIssueService.issue(coupon.getId(), request))
                .isInstanceOf(BusinessException.class).hasMessageContaining("過期");
    }
}
