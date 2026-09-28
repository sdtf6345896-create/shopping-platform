package com.example.shopping.coupon.repository;

import com.example.shopping.coupon.entity.MemberCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    @Query("select mc from MemberCoupon mc join fetch mc.coupon where mc.memberId = :memberId order by mc.claimedAt desc")
    List<MemberCoupon> findByMemberIdWithCoupon(@Param("memberId") Long memberId);

    @Query("select mc.coupon.id from MemberCoupon mc where mc.memberId = :memberId")
    List<Long> findClaimedCouponIds(@Param("memberId") Long memberId);

    @Query("select mc.memberId from MemberCoupon mc where mc.coupon.id = :couponId and mc.memberId in :memberIds")
    List<Long> findHolderIds(@Param("couponId") Long couponId, @Param("memberIds") Collection<Long> memberIds);

    @Modifying
    @Query("delete from MemberCoupon mc where mc.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}
