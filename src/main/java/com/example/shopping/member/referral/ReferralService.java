package com.example.shopping.member.referral;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.order.event.OrderCompletedEvent;
import com.example.shopping.points.service.PointService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;

/**
 * 邀請好友:被邀請的新會員第一筆達門檻的訂單完成後,邀請人與新會員各得購物金(每位新會員只發一次)。
 * 等到訂單完成(已收貨)才發,降低開小帳號下單又取消來刷點數的誘因。
 */
@Service
@Transactional
public class ReferralService {

    /** 去掉容易看錯的 0/O、1/I/L */
    private static final String CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;

    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final int referrerPoints;
    private final int refereePoints;
    private final BigDecimal minOrderAmount;
    private final SecureRandom random = new SecureRandom();

    public ReferralService(MemberRepository memberRepository,
                           PointService pointService,
                           NotificationService notificationService,
                           @Value("${app.referral.referrer-points:100}") int referrerPoints,
                           @Value("${app.referral.referee-points:50}") int refereePoints,
                           @Value("${app.referral.min-order-amount:300}") BigDecimal minOrderAmount) {
        this.memberRepository = memberRepository;
        this.pointService = pointService;
        this.notificationService = notificationService;
        this.referrerPoints = referrerPoints;
        this.refereePoints = refereePoints;
        this.minOrderAmount = minOrderAmount;
    }

    /** 我的邀請碼與成果;還沒有邀請碼就產生一個 */
    public ReferralResponse getMyReferral(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("會員不存在");
        }
        String code = memberRepository.findReferralCodeById(memberId);
        if (code == null) {
            code = assignCode(memberId);
        }
        return new ReferralResponse(code,
                memberRepository.countByReferredById(memberId),
                memberRepository.countByReferredByIdAndReferralRewardedTrue(memberId),
                referrerPoints, refereePoints, minOrderAmount);
    }

    @EventListener
    public void onOrderCompleted(OrderCompletedEvent event) {
        if (event.merchandiseAmount().compareTo(minOrderAmount) < 0) {
            return;
        }
        // 條件式 UPDATE 認領:沒有邀請人或已發過都會回 0,併發下也只會成功一次
        if (memberRepository.claimReferralReward(event.memberId()) == 0) {
            return;
        }
        Member referee = memberRepository.findById(event.memberId()).orElseThrow();
        Member referrer = memberRepository.findById(referee.getReferredById()).orElse(null);

        if (refereePoints > 0) {
            pointService.credit(referee.getId(), event.orderId(), refereePoints, PointTransactionType.REFERRAL,
                    "好友邀請獎勵(首筆訂單 " + event.orderNo() + " 完成)");
            notificationService.notify(referee.getId(), NotificationType.SYSTEM,
                    "獲得好友邀請獎勵 " + refereePoints + " 點",
                    "感謝透過好友邀請加入!首筆訂單已完成,購物金已入帳。", "/member/points");
        }
        if (referrer != null && referrer.getStatus() == AccountStatus.ACTIVE && referrerPoints > 0) {
            pointService.credit(referrer.getId(), null, referrerPoints, PointTransactionType.REFERRAL,
                    "邀請好友 " + maskName(referee.getName()) + " 完成首筆訂單");
            notificationService.notify(referrer.getId(), NotificationType.SYSTEM,
                    "邀請成功!獲得 " + referrerPoints + " 點購物金",
                    "你邀請的好友 " + maskName(referee.getName()) + " 完成了第一筆訂單,謝謝你的推薦!", "/member/referral");
        }
    }

    private String assignCode(Long memberId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            String candidate = randomCode();
            if (!memberRepository.existsByReferralCode(candidate)) {
                memberRepository.assignReferralCode(memberId, candidate);
                // 併發時可能是另一個請求先寫入,一律以資料庫裡的為準
                return memberRepository.findReferralCodeById(memberId);
            }
        }
        throw new IllegalStateException("無法產生不重複的邀請碼");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 通知邀請人時只露出姓氏,例如「王**」 */
    static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "好友";
        }
        return name.charAt(0) + "*".repeat(Math.max(1, Math.min(name.length() - 1, 2)));
    }
}
