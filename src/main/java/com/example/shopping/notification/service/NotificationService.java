package com.example.shopping.notification.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.notification.dto.NotificationResponse;
import com.example.shopping.notification.entity.Notification;
import com.example.shopping.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /** 建立一則站內通知(跟著呼叫端的交易,訂單變更回滾時通知也不會留下) */
    public void notify(Long memberId, NotificationType type, String title, String content, String link) {
        Notification notification = new Notification();
        notification.setMemberId(memberId);
        notification.setType(type);
        notification.setTitle(truncate(title, 100));
        notification.setContent(truncate(content, 500));
        notification.setLink(link);
        repository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> list(Long memberId, Pageable pageable) {
        return repository.findByMemberId(memberId, pageable).map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        return repository.countByMemberIdAndReadAtIsNull(memberId);
    }

    public NotificationResponse markRead(Long memberId, Long notificationId) {
        Notification notification = repository.findByIdAndMemberId(notificationId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("通知不存在"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }
        return NotificationResponse.from(notification);
    }

    public int markAllRead(Long memberId) {
        return repository.markAllRead(memberId, LocalDateTime.now());
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
