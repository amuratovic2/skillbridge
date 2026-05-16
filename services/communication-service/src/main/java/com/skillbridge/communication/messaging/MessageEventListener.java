package com.skillbridge.communication.messaging;

import com.rabbitmq.client.Channel;
import com.skillbridge.communication.config.RabbitMQConfig;
import com.skillbridge.communication.model.NotificationType;
import com.skillbridge.communication.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * Consumes message.sent events and produces the receiver's notification.
 *
 * Pairing this with the publisher gives one publish + one deliver on the
 * RabbitMQ rate chart per chat message, so both sides of the conversation
 * register as broker activity.
 */
@Component
public class MessageEventListener {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);

    private final NotificationService notificationService;

    public MessageEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.MESSAGE_RECEIVED_QUEUE)
    public void handle(
        Map<String, Object> payload,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            Integer senderId = asInteger(payload.get("senderId"));
            Integer receiverId = asInteger(payload.get("receiverId"));
            Integer orderId = asInteger(payload.get("orderId"));
            String preview = asString(payload.get("preview"));

            if (receiverId != null) {
                notificationService.create(
                    receiverId,
                    NotificationType.NEW_MESSAGE,
                    "Nova poruka od korisnika #" + senderId,
                    preview == null ? "" : preview,
                    orderId
                );
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to handle message.sent event: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
