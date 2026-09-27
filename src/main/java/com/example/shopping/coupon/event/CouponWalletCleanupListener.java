package com.example.shopping.coupon.event;

import com.example.shopping.coupon.repository.MemberCouponRepository;
import com.example.shopping.member.event.MemberDeletingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 會員刪除帳號時清除已領取的優惠券 */
@Component
public class CouponWalletCleanupListener {

    private final MemberCouponRepository memberCouponRepository;

    public CouponWalletCleanupListener(MemberCouponRepository memberCouponRepository) {
        this.memberCouponRepository = memberCouponRepository;
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        memberCouponRepository.deleteAllByMemberId(event.memberId());
    }
}
