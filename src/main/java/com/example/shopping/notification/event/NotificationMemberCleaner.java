package com.example.shopping.notification.event;

import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.notification.repository.NotificationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 會員刪除帳號時清除其站內通知 */
@Component
public class NotificationMemberCleaner {

    private final NotificationRepository notificationRepository;

    public NotificationMemberCleaner(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        notificationRepository.deleteAllByMemberId(event.memberId());
    }
}
