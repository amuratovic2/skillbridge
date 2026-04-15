package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.service.NotificationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<?> findByUser(
        @RequestHeader("x-user-id") Integer userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var result = notificationService.findByUser(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<?> markAsRead(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(notificationService.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> markAllAsRead(@RequestHeader("x-user-id") Integer userId) {
        int updated = notificationService.markAllAsRead(userId);
        return ApiResponse.ok(java.util.Map.of("updated", updated));
    }
}
