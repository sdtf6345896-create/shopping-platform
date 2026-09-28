package com.example.shopping;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.referral.ReferralResponse;
import com.example.shopping.member.referral.ReferralService;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.service.MemberService;
import com.example.shopping.order.event.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 邀請碼註冊 → 首筆達門檻的訂單完成時雙方各得購物金,只發一次;門檻以下不發;無效邀請碼擋下 */
@SpringBootTest
@ActiveProfiles("test")
class ReferralIntegrationTest {

    @Autowired
    private ReferralService referralService;
    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private Member member(String name) {
        Member member = new Member();
        member.setEmail(name + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword("x");
        member.setName(name);
        member.setEmailVerified(true);
        return memberRepository.save(member);
    }

    private RegisterRequest registration(String referralCode) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        request.setPassword("password123");
        request.setName("新朋友");
        request.setReferralCode(referralCode);
        return request;
    }

    private void complete(Long memberId, String merchandise) {
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(
                new OrderCompletedEvent(memberId, null, "REF-ORDER", new BigDecimal(merchandise))));
    }

    @Test
    void rewardsBothSidesOnceAfterFirstQualifyingOrder() {
        Member referrer = member("王大明");
        ReferralResponse mine = referralService.getMyReferral(referrer.getId());
        assertThat(mine.code()).hasSize(8);
        assertThat(referralService.getMyReferral(referrer.getId()).code()).isEqualTo(mine.code());

        // 邀請碼不分大小寫、前後空白
        MemberResponse friend = memberService.register(registration("  " + mine.code().toLowerCase() + " "));
        assertThat(referralService.getMyReferral(referrer.getId()).invitedCount()).isEqualTo(1);

        complete(friend.getId(), "299");   // 未達 300 門檻:不發
        assertThat(memberRepository.findPointsById(friend.getId())).isZero();

        complete(friend.getId(), "300");
        assertThat(memberRepository.findPointsById(friend.getId())).isEqualTo(mine.refereePoints());
        assertThat(memberRepository.findPointsById(referrer.getId())).isEqualTo(mine.referrerPoints());

        complete(friend.getId(), "5000");  // 之後的訂單不再發
        assertThat(memberRepository.findPointsById(friend.getId())).isEqualTo(mine.refereePoints());
        assertThat(memberRepository.findPointsById(referrer.getId())).isEqualTo(mine.referrerPoints());
        assertThat(referralService.getMyReferral(referrer.getId()).rewardedCount()).isEqualTo(1);
    }

    @Test
    void ordersFromMembersWithoutReferrerAreIgnored_andUnknownCodesRejected() {
        Member loner = member("獨行俠");
        complete(loner.getId(), "1000");
        assertThat(memberRepository.findPointsById(loner.getId())).isZero();

        assertThatThrownBy(() -> memberService.register(registration("NOPE2345")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("邀請碼無效");
    }
}
