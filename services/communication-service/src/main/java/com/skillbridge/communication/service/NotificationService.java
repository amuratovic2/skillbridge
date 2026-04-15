package com.skillbridge.communication.service;

import com.skillbridge.communication.model.Notification;
import com.skillbridge.communication.model.NotificationType;
import com.skillbridge.communication.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(Integer userId, NotificationType type, String title, String content, Integer referenceId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReferenceId(referenceId);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public Map<String, Object> findByUser(Integer userId, int page, int limit) {
        Page<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            userId, PageRequest.of(page - 1, limit)
        );

        return Map.of(
            "data", result.getContent(),
            "meta", Map.of(
                "total", result.getTotalElements(),
                "page", page,
                "limit", limit,
                "totalPages", result.getTotalPages()
            )
        );
    }

    public Notification markAsRead(Integer id, Integer userId) {
        Notification notification = notificationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new IllegalStateException("Notification does not belong to this user");
        }
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(Integer userId) {
        return notificationRepository.markAllAsRead(userId);
    }

    public Map<String, Object> getUnreadCount(Integer userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        return Map.of("count", count);
    }
}
