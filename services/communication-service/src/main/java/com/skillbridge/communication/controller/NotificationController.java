package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse<?> findByUser(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        var result = notificationService.findByUser(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(@RequestHeader("x-user-id") @Positive Integer userId) {
        return ApiResponse.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<?> markAsRead(
        @PathVariable @Positive Integer id,
        @RequestHeader("x-user-id") @Positive Integer userId
    ) {
        return ApiResponse.ok(notificationService.markAsRead(id, userId));
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> markAllAsRead(@RequestHeader("x-user-id") @Positive Integer userId) {
        int updated = notificationService.markAllAsRead(userId);
        return ApiResponse.ok(java.util.Map.of("updated", updated));
    }
}
