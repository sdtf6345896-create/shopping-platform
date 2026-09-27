package com.example.shopping.notification.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.notification.dto.NotificationResponse;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(
                notificationService.list(SecurityUtils.getCurrentUserId(), pageable)));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.success(Map.of("count", notificationService.unreadCount(SecurityUtils.getCurrentUserId())));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable Long id) {
        return ApiResponse.success(notificationService.markRead(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping("/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        return ApiResponse.success(Map.of("updated", notificationService.markAllRead(SecurityUtils.getCurrentUserId())));
    }
}
