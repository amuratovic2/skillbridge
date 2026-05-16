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
 * Translates async events from order-service into per-user notifications.
 *
 * Bindings are wildcard ({@code order.*}, {@code custom-offer.*},
 * {@code delivery.*}) so any new lifecycle event automatically lands in the
 * right queue and we route it from the {@code eventType} field.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.ORDER_NOTIFICATIONS_QUEUE)
    public void handleOrderEvent(
        Map<String, Object> payload,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            String type = asString(payload.get("eventType"));
            Integer orderId = asInteger(payload.get("orderId"));
            Integer clientId = asInteger(payload.get("clientId"));
            Integer sellerId = asInteger(payload.get("sellerId"));
            String oldStatus = asString(payload.get("oldStatus"));
            String newStatus = asString(payload.get("newStatus"));
            String note = asString(payload.get("note"));

            switch (type) {
                case "order.placed" -> notify(sellerId, "Nova narudžba čeka potvrdu",
                    "Klijent je naručio uslugu. Prihvatite ili odbijte.", orderId);
                case "order.accepted" -> notify(clientId, "Narudžba prihvaćena",
                    statusLine(orderId, oldStatus, newStatus), orderId);
                case "order.in-progress" -> notify(clientId, "Rad je započet",
                    statusLine(orderId, oldStatus, newStatus), orderId);
                case "order.delivered" -> notify(clientId, "Nova isporuka",
                    note != null && !note.isBlank() ? note
                        : "Freelancer je isporučio rad. Prihvatite ili tražite reviziju.",
                    orderId);
                case "order.completed" -> {
                    notify(clientId, "Narudžba završena", statusLine(orderId, oldStatus, newStatus), orderId);
                    notify(sellerId, "Narudžba završena", statusLine(orderId, oldStatus, newStatus), orderId);
                }
                case "order.cancelled" -> {
                    notify(clientId, "Narudžba otkazana", statusLine(orderId, oldStatus, newStatus), orderId);
                    notify(sellerId, "Narudžba otkazana", statusLine(orderId, oldStatus, newStatus), orderId);
                }
                case "order.revision-requested" -> notify(sellerId, "Tražena revizija",
                    note != null && !note.isBlank() ? note
                        : "Klijent traži izmjene na isporuci.",
                    orderId);
                default -> log.debug("Ignored order event type: {}", type);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to handle order event: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.OFFER_NOTIFICATIONS_QUEUE)
    public void handleOfferEvent(
        Map<String, Object> payload,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            String type = asString(payload.get("eventType"));
            Integer offerId = asInteger(payload.get("offerId"));
            Integer senderId = asInteger(payload.get("senderId"));
            Integer receiverId = asInteger(payload.get("receiverId"));
            String title = asString(payload.get("title"));

            switch (type) {
                case "custom-offer.sent" -> notify(receiverId, NotificationType.CUSTOM_OFFER,
                    "Nova prilagođena ponuda",
                    "Primili ste novu ponudu" + (title != null ? " \"" + title + "\"" : "") + ".",
                    offerId);
                case "custom-offer.accepted" -> notify(senderId, NotificationType.CUSTOM_OFFER,
                    "Ponuda prihvaćena",
                    "Vaša ponuda" + (title != null ? " \"" + title + "\"" : "") + " je sada ACCEPTED.",
                    offerId);
                case "custom-offer.rejected" -> notify(senderId, NotificationType.CUSTOM_OFFER,
                    "Ponuda odbijena",
                    "Vaša ponuda" + (title != null ? " \"" + title + "\"" : "") + " je odbijena.",
                    offerId);
                case "custom-offer.withdrawn" -> notify(receiverId, NotificationType.CUSTOM_OFFER,
                    "Ponuda povučena",
                    "Pošiljatelj je povukao ponudu" + (title != null ? " \"" + title + "\"" : "") + ".",
                    offerId);
                default -> log.debug("Ignored offer event type: {}", type);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to handle offer event: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DELIVERY_NOTIFICATIONS_QUEUE)
    public void handleDeliveryEvent(
        Map<String, Object> payload,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {
        try {
            Integer orderId = asInteger(payload.get("orderId"));
            Integer clientId = asInteger(payload.get("clientId"));
            Integer version = asInteger(payload.get("versionNumber"));
            notify(clientId, "Nova isporuka (#" + orderId + ")",
                "Freelancer je isporučio rad – verzija " + version + ". Prihvatite ili tražite reviziju.",
                orderId);
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to handle delivery event: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void notify(Integer userId, String title, String content, Integer referenceId) {
        notify(userId, NotificationType.ORDER_UPDATE, title, content, referenceId);
    }

    private void notify(Integer userId, NotificationType type, String title, String content, Integer referenceId) {
        if (userId == null) return;
        notificationService.create(userId, type, title, content, referenceId);
    }

    private String statusLine(Integer orderId, String oldStatus, String newStatus) {
        if (oldStatus == null || newStatus == null) return "Narudžba #" + orderId;
        return "Narudžba #" + orderId + ": " + oldStatus + " → " + newStatus;
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
