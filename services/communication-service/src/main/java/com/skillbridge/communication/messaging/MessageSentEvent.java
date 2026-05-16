package com.skillbridge.communication.messaging;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Published whenever a chat message is persisted. The local listener consumes
 * it and creates the receiver's notification — this routes every send through
 * RabbitMQ so the publish + consume rates are both visible on the broker's
 * message-rate chart.
 */
public record MessageSentEvent(
    Integer messageId,
    Integer senderId,
    Integer receiverId,
    Integer orderId,
    String preview,
    LocalDateTime occurredAt
) implements Serializable {

    public static MessageSentEvent of(Integer messageId, Integer senderId, Integer receiverId,
                                      Integer orderId, String preview) {
        return new MessageSentEvent(messageId, senderId, receiverId, orderId, preview, LocalDateTime.now());
    }
}
