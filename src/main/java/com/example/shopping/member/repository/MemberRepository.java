package com.example.shopping.member.repository;

import com.example.shopping.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, JpaSpecificationExecutor<Member> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    /** 餘額足夠才扣除,回傳受影響筆數(0 = 餘額不足) */
    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.points = m.points - :amount where m.id = :memberId and m.points >= :amount")
    int deductPoints(@Param("memberId") Long memberId, @Param("amount") int amount);

    @Modifying(flushAutomatically = true)
    @Query("update Member m set m.points = m.points + :amount where m.id = :memberId")
    int addPoints(@Param("memberId") Long memberId, @Param("amount") int amount);

    /** 直接查資料庫的最新餘額(不經過可能過期的一級快取) */
    @Query("select m.points from Member m where m.id = :memberId")
    Integer findPointsById(@Param("memberId") Long memberId);
}
