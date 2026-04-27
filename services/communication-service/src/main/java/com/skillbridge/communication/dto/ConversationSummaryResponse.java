package com.skillbridge.communication.dto;

import java.time.LocalDateTime;

public record ConversationSummaryResponse(
    Integer partnerId,
    String lastMessage,
    LocalDateTime lastAt,
    int unreadCount
) {
}
