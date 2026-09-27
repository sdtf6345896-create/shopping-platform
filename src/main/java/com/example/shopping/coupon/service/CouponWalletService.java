package com.example.shopping.coupon.service;

import com.example.shopping.common.enums.CouponStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.coupon.dto.response.WalletCouponResponse;
import com.example.shopping.coupon.entity.Coupon;
import com.example.shopping.coupon.entity.MemberCoupon;
import com.example.shopping.coupon.repository.CouponRepository;
import com.example.shopping.coupon.repository.MemberCouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 領券中心與「我的優惠券」 */
@Service
@Transactional
public class CouponWalletService {

    private final CouponRepository couponRepository;
    private final MemberCouponRepository memberCouponRepository;
    private final CouponUsageLookup couponUsageLookup;

    public CouponWalletService(CouponRepository couponRepository,
                               MemberCouponRepository memberCouponRepository,
                               CouponUsageLookup couponUsageLookup) {
        this.couponRepository = couponRepository;
        this.memberCouponRepository = memberCouponRepository;
        this.couponUsageLookup = couponUsageLookup;
    }

    @Transactional(readOnly = true)
    public List<WalletCouponResponse> listClaimable(Long memberId) {
        Set<Long> claimed = new HashSet<>(memberCouponRepository.findClaimedCouponIds(memberId));
        return couponRepository.findClaimable(CouponStatus.ACTIVE, LocalDateTime.now()).stream()
                .map(coupon -> WalletCouponResponse.from(coupon, claimed.contains(coupon.getId())))
                .toList();
    }

    /** 領取優惠券;重複領取不會重複建立 */
    public WalletCouponResponse claim(Long memberId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ResourceNotFoundException("優惠券不存在"));
        if (!coupon.isClaimable() || !isUsableNow(coupon)) {
            throw new BusinessException("此優惠券目前無法領取");
        }
        if (!memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            MemberCoupon memberCoupon = new MemberCoupon();
            memberCoupon.setMemberId(memberId);
            memberCoupon.setCoupon(coupon);
            memberCouponRepository.save(memberCoupon);
        }
        return WalletCouponResponse.from(coupon, true);
    }

    /** 我的優惠券:已領取且目前還能用的(未過期、未停用、未發完、未用滿個人次數) */
    @Transactional(readOnly = true)
    public List<WalletCouponResponse> listMine(Long memberId) {
        return memberCouponRepository.findByMemberIdWithCoupon(memberId).stream()
                .map(MemberCoupon::getCoupon)
                .filter(this::isUsableNow)
                .filter(coupon -> coupon.getPerMemberLimit() == null
                        || couponUsageLookup.countUsedBy(memberId, coupon.getId()) < coupon.getPerMemberLimit())
                .map(coupon -> WalletCouponResponse.from(coupon, true))
                .toList();
    }

    private boolean isUsableNow(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        return coupon.getStatus() == CouponStatus.ACTIVE
                && (coupon.getStartAt() == null || !now.isBefore(coupon.getStartAt()))
                && (coupon.getEndAt() == null || now.isBefore(coupon.getEndAt()))
                && (coupon.getTotalQuantity() == null || coupon.getUsedQuantity() < coupon.getTotalQuantity());
    }
}
