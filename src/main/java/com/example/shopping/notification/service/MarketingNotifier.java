package com.example.shopping.notification.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.member.repository.MemberRepository;
import org.springframework.stereotype.Component;

/**
 * 行銷類站內通知(購物車提醒、收藏商品特價、發券通知):會員關閉「接收行銷通知」時不發送。
 * 訂單、退貨、問答回覆、購物金異動等交易通知請直接用 {@link NotificationService}。
 */
@Component
public class MarketingNotifier {

    private final NotificationService notificationService;
    private final MemberRepository memberRepository;

    public MarketingNotifier(NotificationService notificationService, MemberRepository memberRepository) {
        this.notificationService = notificationService;
        this.memberRepository = memberRepository;
    }

    /** @return 是否有發出(會員關閉行銷通知或不存在時為 false) */
    public boolean notify(Long memberId, String title, String content, String link) {
        if (!Boolean.TRUE.equals(memberRepository.findMarketingOptInById(memberId))) {
            return false;
        }
        notificationService.notify(memberId, NotificationType.SYSTEM, title, content, link);
        return true;
    }
}
