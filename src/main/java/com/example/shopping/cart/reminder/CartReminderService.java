package com.example.shopping.cart.reminder;

import com.example.shopping.cart.repository.CartItemRepository;
import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 購物車提醒:購物車放著超過 idleHours 小時沒動,發一則站內通知。
 * 同一個購物車狀態只提醒一次(之後會員再加入或修改商品、又放著不動,才會再提醒)。
 */
@Service
@Transactional
public class CartReminderService {

    private final CartItemRepository cartItemRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;
    private final long idleHours;

    public CartReminderService(CartItemRepository cartItemRepository,
                               MemberRepository memberRepository,
                               NotificationService notificationService,
                               @Value("${app.cart.reminder-idle-hours:24}") long idleHours) {
        this.cartItemRepository = cartItemRepository;
        this.memberRepository = memberRepository;
        this.notificationService = notificationService;
        this.idleHours = idleHours;
    }

    /** @return 發出的提醒數;idleHours 設為 0 以下表示關閉 */
    public int sendReminders(LocalDateTime now) {
        if (idleHours <= 0) {
            return 0;
        }
        int sent = 0;
        for (CartItemRepository.IdleCart cart : cartItemRepository.findIdleCarts(now.minusHours(idleHours))) {
            // 條件式 UPDATE 認領,多台機器同時跑也只會發一次
            if (memberRepository.claimCartReminder(cart.getMemberId(), now, cart.getLastActivity()) == 0) {
                continue;
            }
            notificationService.notify(cart.getMemberId(), NotificationType.SYSTEM, "購物車裡的商品還在等你",
                    "你的購物車還有 " + cart.getItemCount() + " 項商品尚未結帳,熱門商品庫存有限,別錯過了!",
                    "/cart");
            sent++;
        }
        return sent;
    }
}
