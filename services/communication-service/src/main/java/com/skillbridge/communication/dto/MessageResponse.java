package com.skillbridge.communication.dto;

import java.time.LocalDateTime;

public record MessageResponse(
    Integer id,
    Integer senderId,
    Integer receiverId,
    Integer orderId,
    String content,
    boolean isRead,
    LocalDateTime sentAt
) {
}
