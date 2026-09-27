package com.example.shopping.notification.dto;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private String link;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getContent(), n.getLink(),
                n.getReadAt() != null, n.getCreatedAt());
    }
}
