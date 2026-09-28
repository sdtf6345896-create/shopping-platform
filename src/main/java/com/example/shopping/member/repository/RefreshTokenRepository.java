package com.example.shopping.member.repository;

import com.example.shopping.member.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** 撤銷會員所有 refresh token(改密碼後其他裝置需重新登入) */
    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.memberId = :memberId and t.revoked = false")
    int revokeAllByMemberId(@Param("memberId") Long memberId);

    /** 會員目前有效的 token(每個登入工作階段只會有一個有效的,輪替時舊的會被撤銷) */
    @Query("select t from RefreshToken t where t.memberId = :memberId and t.revoked = false and t.expiresAt > :now"
            + " order by t.lastUsedAt desc, t.id desc")
    List<RefreshToken> findActiveByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.memberId = :memberId and t.sessionId = :sessionId"
            + " and t.revoked = false")
    int revokeSession(@Param("memberId") Long memberId, @Param("sessionId") String sessionId);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.memberId = :memberId and t.sessionId <> :keepSessionId"
            + " and t.revoked = false")
    int revokeOtherSessions(@Param("memberId") Long memberId, @Param("keepSessionId") String keepSessionId);
}
