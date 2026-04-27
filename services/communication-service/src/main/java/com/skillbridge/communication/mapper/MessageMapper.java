package com.skillbridge.communication.mapper;

import com.skillbridge.communication.dto.MessageResponse;
import com.skillbridge.communication.model.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getSenderId(),
            message.getReceiverId(),
            message.getOrderId(),
            message.getContent(),
            message.getIsRead(),
            message.getSentAt()
        );
    }
}
