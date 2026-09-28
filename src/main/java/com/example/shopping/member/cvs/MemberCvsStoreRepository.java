package com.example.shopping.member.cvs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCvsStoreRepository extends JpaRepository<MemberCvsStore, Long> {

    List<MemberCvsStore> findByMemberIdOrderByIdDesc(Long memberId);

    Optional<MemberCvsStore> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberId(Long memberId);

    @Modifying
    @Query("delete from MemberCvsStore s where s.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}
