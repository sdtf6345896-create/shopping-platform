package com.example.shopping.member.repository;

import com.example.shopping.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** 撤銷會員所有 refresh token(改密碼後其他裝置需重新登入) */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.memberId = :memberId and t.revoked = false")
    int revokeAllByMemberId(@Param("memberId") Long memberId);
}
