package com.example.shopping.member.repository;

import com.example.shopping.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    /** 餘額足夠才扣除,回傳受影響筆數(0 = 餘額不足) */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.points = m.points - :amount where m.id = :memberId and m.points >= :amount")
    int deductPoints(@Param("memberId") Long memberId, @Param("amount") int amount);

    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.points = m.points + :amount where m.id = :memberId")
    int addPoints(@Param("memberId") Long memberId, @Param("amount") int amount);

    @Query("select m.id from Member m where m.status = com.example.shopping.common.enums.AccountStatus.ACTIVE")
    List<Long> findActiveIds();

    /** 依 Email(不分大小寫)找啟用中的會員 */
    @Query("select m from Member m where m.status = com.example.shopping.common.enums.AccountStatus.ACTIVE"
            + " and lower(m.email) in :emails")
    List<Member> findActiveByEmailsIgnoreCase(@Param("emails") Collection<String> emails);

    @Query("select m from Member m where m.status = com.example.shopping.common.enums.AccountStatus.ACTIVE"
            + " and m.referralCode = :code")
    Optional<Member> findActiveByReferralCode(@Param("code") String code);

    boolean existsByReferralCode(String referralCode);

    @Query("select m.referralCode from Member m where m.id = :memberId")
    String findReferralCodeById(@Param("memberId") Long memberId);

    /** 還沒有邀請碼時才寫入,回傳受影響筆數(0 = 已經有了) */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.referralCode = :code where m.id = :memberId and m.referralCode is null")
    int assignReferralCode(@Param("memberId") Long memberId, @Param("code") String code);

    long countByReferredById(Long referrerId);

    long countByReferredByIdAndReferralRewardedTrue(Long referrerId);

    /** 認領邀請獎勵:被邀請且尚未發放才更新,回傳受影響筆數(0 = 不適用或已發過) */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.referralRewarded = true where m.id = :memberId"
            + " and m.referredById is not null and m.referralRewarded = false")
    int claimReferralReward(@Param("memberId") Long memberId);

    /** 認領購物車提醒:這次購物車異動之後還沒提醒過才更新,回傳受影響筆數(0 = 已提醒過) */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.cartRemindedAt = :now where m.id = :memberId"
            + " and (m.cartRemindedAt is null or m.cartRemindedAt < :lastActivity)")
    int claimCartReminder(@Param("memberId") Long memberId, @Param("now") LocalDateTime now,
                          @Param("lastActivity") LocalDateTime lastActivity);

    @Query("select m.marketingOptIn from Member m where m.id = :memberId")
    Boolean findMarketingOptInById(@Param("memberId") Long memberId);

    /** 當月壽星中今年還沒領過生日禮的啟用會員 id */
    @Query("select m.id from Member m where m.status = com.example.shopping.common.enums.AccountStatus.ACTIVE"
            + " and m.birthday is not null and extract(month from m.birthday) = :month"
            + " and (m.birthdayRewardYear is null or m.birthdayRewardYear < :year)")
    List<Long> findBirthdayRewardCandidates(@Param("month") int month, @Param("year") int year);

    /** 認領今年的生日禮:今年還沒領過才更新,回傳受影響筆數(0 = 已領過),併發下也只會成功一次 */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.birthdayRewardYear = :year where m.id = :memberId"
            + " and (m.birthdayRewardYear is null or m.birthdayRewardYear < :year)")
    int claimBirthdayReward(@Param("memberId") Long memberId, @Param("year") int year);

    /** 直接查資料庫的最新餘額(不經過可能過期的一級快取) */
    @Query("select m.points from Member m where m.id = :memberId")
    Integer findPointsById(@Param("memberId") Long memberId);
}
