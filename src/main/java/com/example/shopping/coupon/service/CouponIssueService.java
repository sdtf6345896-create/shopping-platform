package com.example.shopping.coupon.service;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.coupon.dto.request.CouponIssueRequest;
import com.example.shopping.coupon.dto.response.CouponIssueResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.entity.MemberCoupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.repository.MemberCouponRepository;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.tier.MemberTier;
import com.example.shopping.member.tier.MemberTierService;
import com.example.shopping.notification.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 後台發券:把優惠券直接放進會員的「我的優惠券」並發站內通知。
 * 已持有的會員略過(不重複發、不重複通知),所以同一張券可以安心重複發給同一群人補漏。
 */
@Service
@Transactional
public class CouponIssueService {

    /** IN 子句分批大小,避免名單很大時 SQL 過長 */
    static final int CHUNK_SIZE = 1000;

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final MemberTierService memberTierService;
    private final NotificationService notificationService;

    public CouponIssueService(CouponRepository couponRepository,
                              MemberCouponRepository memberCouponRepository,
                              MemberRepository memberRepository,
                              MemberTierService memberTierService,
                              NotificationService notificationService) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.memberRepository = memberRepository;
        this.memberTierService = memberTierService;
        this.notificationService = notificationService;
    }

    public CouponIssueResponse issue(Long couponId, CouponIssueRequest request) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("優惠券不存在"));
        requireIssuable(coupon);

        List<String> unmatchedEmails = new ArrayList<>();
        Set<Long> memberIds = resolveTargets(request, unmatchedEmails);

        Set<Long> holders = new HashSet<>();
        List<Long> ids = new ArrayList<>(memberIds);
        for (int from = 0; from < ids.size(); from += CHUNK_SIZE) {
            holders.addAll(memberCouponRepository.findHolderIds(couponId,
                    ids.subList(from, Math.min(from + CHUNK_SIZE, ids.size()))));
        }

        List<MemberCoupon> newHolders = new ArrayList<>();
        for (Long memberId : memberIds) {
            if (holders.contains(memberId)) {
                continue;
            }
            MemberCoupon memberCoupon = new MemberCoupon();
            memberCoupon.setMemberId(memberId);
            memberCoupon.setCoupon(coupon);
            newHolders.add(memberCoupon);
        }
        memberCouponRepository.saveAll(newHolders);

        String title = "你獲得一張優惠券:" + coupon.getName();
        String content = "優惠碼 " + coupon.getCode() + " 已放進「我的優惠券」,結帳時輸入即可使用。";
        for (MemberCoupon memberCoupon : newHolders) {
            notificationService.notify(memberCoupon.getMemberId(), NotificationType.SYSTEM, title, content,
                    "/member/coupons");
        }
        return new CouponIssueResponse(memberIds.size(), newHolders.size(), memberIds.size() - newHolders.size(),
                unmatchedEmails);
    }

    private Set<Long> resolveTargets(CouponIssueRequest request, List<String> unmatchedEmails) {
        return switch (request.getTarget()) {
            case ALL -> new LinkedHashSet<>(memberRepository.findActiveIds());
            case TIER -> {
                MemberTier tier = request.getMinTier();
                if (tier == null) {
                    throw new BusinessException("請選擇會員等級");
                }
                yield new LinkedHashSet<>(tier.ordinal() == 0
                        ? memberRepository.findActiveIds()
                        : memberTierService.activeMemberIdsAtLeast(tier));
            }
            case EMAILS -> byEmails(request.getEmails(), unmatchedEmails);
        };
    }

    private Set<Long> byEmails(List<String> rawEmails, List<String> unmatchedEmails) {
        Set<String> emails = new LinkedHashSet<>();
        if (rawEmails != null) {
            for (String email : rawEmails) {
                if (email != null && !email.isBlank()) {
                    emails.add(email.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (emails.isEmpty()) {
            throw new BusinessException("請輸入至少一個會員 Email");
        }
        Set<Long> ids = new LinkedHashSet<>();
        Set<String> found = new HashSet<>();
        for (Member member : memberRepository.findActiveByEmailsIgnoreCase(emails)) {
            ids.add(member.getId());
            found.add(member.getEmail().toLowerCase(Locale.ROOT));
        }
        emails.stream().filter(email -> !found.contains(email)).forEach(unmatchedEmails::add);
        return ids;
    }

    private static void requireIssuable(Coupon coupon) {
        if (coupon.getStatus() != CouponStatus.ACTIVE) {
            throw new BusinessException("優惠券已停用,無法發放");
        }
        if (coupon.getEndAt() != null && !LocalDateTime.now().isBefore(coupon.getEndAt())) {
            throw new BusinessException("優惠券已過期,無法發放");
        }
        if (coupon.getTotalQuantity() != null && coupon.getUsedQuantity() >= coupon.getTotalQuantity()) {
            throw new BusinessException("優惠券已兌換完畢,無法發放");
        }
    }
}
