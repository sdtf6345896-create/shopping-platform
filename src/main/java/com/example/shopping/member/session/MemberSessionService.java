package com.example.shopping.member.session;

import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.entity.RefreshToken;
import com.example.shopping.member.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登入裝置管理:列出會員目前有效的登入工作階段,可登出單一裝置或登出其他所有裝置。
 * 撤銷的是 refresh token;該裝置手上的 access token 仍可用到過期為止(通常數分鐘),之後就無法再換發。
 */
@Service
@Transactional
public class MemberSessionService {

    private final RefreshTokenRepository refreshTokenRepository;

    public MemberSessionService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public record SessionResponse(String sessionId, String userAgent, String ipAddress, LocalDateTime lastUsedAt,
                                  LocalDateTime expiresAt) {

        static SessionResponse from(RefreshToken token) {
            return new SessionResponse(token.getSessionId(), token.getUserAgent(), token.getIpAddress(),
                    token.getLastUsedAt(), token.getExpiresAt());
        }
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> list(Long memberId) {
        return refreshTokenRepository.findActiveByMemberId(memberId, LocalDateTime.now()).stream()
                .filter(token -> token.getSessionId() != null)
                .map(SessionResponse::from)
                .toList();
    }

    public void revoke(Long memberId, String sessionId) {
        if (refreshTokenRepository.revokeSession(memberId, sessionId) == 0) {
            throw new ResourceNotFoundException("找不到這個登入裝置,可能已經登出");
        }
    }

    /** @return 登出的裝置數 */
    public int revokeOthers(Long memberId, String currentSessionId) {
        return refreshTokenRepository.revokeOtherSessions(memberId, currentSessionId);
    }
}
