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
import java.util.Objects;

/**
 * Translates async events from order-service into per-user notifications.
 *
 * Each event fans out to both parties involved, but each party sees a
 * different message depending on whether they were the actor who triggered
 * the change or the counterpart who is being informed about it. The actor is
 * identified by the {@code actorId} field on the event payload — if it
 * matches a party, that party gets the actor-perspective copy; otherwise
 * they get the counterpart copy.
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
            Integer actorId = asInteger(payload.get("actorId"));
            String note = asString(payload.get("note"));

            switch (type) {
                case "order.placed" -> {
                    // Client placed the order; freelancer must respond.
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        "Narudžba poslana (#" + orderId + ")",
                        "Vaša narudžba čeka da je freelancer prihvati.",
                        null, null, orderId);
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        null, null,
                        "Nova narudžba čeka potvrdu (#" + orderId + ")",
                        "Klijent je naručio uslugu. Prihvatite ili odbijte.",
                        orderId);
                }
                case "order.accepted" -> {
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        "Prihvatili ste narudžbu (#" + orderId + ")",
                        "Narudžba #" + orderId + " je sada ACCEPTED.",
                        "Narudžba prihvaćena (#" + orderId + ")",
                        "Freelancer je prihvatio narudžbu.",
                        orderId);
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        null, null,
                        "Narudžba prihvaćena (#" + orderId + ")",
                        "Freelancer je prihvatio vašu narudžbu.",
                        orderId);
                }
                case "order.in-progress" -> {
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        "Započeli ste rad (#" + orderId + ")",
                        "Narudžba #" + orderId + " je u izradi.",
                        "Rad je započet (#" + orderId + ")",
                        "Narudžba #" + orderId + " je u izradi.",
                        orderId);
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        null, null,
                        "Rad je započet (#" + orderId + ")",
                        "Freelancer je započeo rad na narudžbi.",
                        orderId);
                }
                case "order.delivered" -> {
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        "Isporuka poslana (#" + orderId + ")",
                        note != null && !note.isBlank()
                            ? note
                            : "Isporuka je poslana klijentu na pregled.",
                        "Nova isporuka (#" + orderId + ")",
                        "Freelancer je isporučio rad. Klijent treba pregledati isporuku.",
                        orderId);
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        null, null,
                        "Nova isporuka (#" + orderId + ")",
                        note != null && !note.isBlank()
                            ? note
                            : "Freelancer je isporučio rad. Prihvatite ili tražite reviziju.",
                        orderId);
                }
                case "order.completed" -> {
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        "Prihvatili ste isporuku (#" + orderId + ")",
                        "Hvala. Narudžba #" + orderId + " je završena.",
                        "Narudžba završena (#" + orderId + ")",
                        "Klijent je prihvatio isporuku.",
                        orderId);
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        "Završili ste narudžbu (#" + orderId + ")",
                        "Narudžba #" + orderId + " je završena.",
                        "Narudžba završena (#" + orderId + ")",
                        "Klijent je prihvatio isporuku.",
                        orderId);
                }
                case "order.cancelled" -> {
                    String cancelNote = note != null && !note.isBlank() ? note : "Narudžba je otkazana.";
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        "Otkazali ste narudžbu (#" + orderId + ")",
                        cancelNote,
                        "Narudžba otkazana (#" + orderId + ")",
                        cancelNote,
                        orderId);
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        "Otkazali ste narudžbu (#" + orderId + ")",
                        cancelNote,
                        "Narudžba otkazana (#" + orderId + ")",
                        cancelNote,
                        orderId);
                }
                case "order.revision-requested" -> {
                    String revNote = note != null && !note.isBlank() ? note : "Klijent traži izmjene na isporuci.";
                    notifyPerspective(clientId, actorId, NotificationType.ORDER_UPDATE,
                        "Zahtjev za revizijom poslan (#" + orderId + ")",
                        revNote,
                        null, null, orderId);
                    notifyPerspective(sellerId, actorId, NotificationType.ORDER_UPDATE,
                        null, null,
                        "Tražena revizija (#" + orderId + ")",
                        revNote,
                        orderId);
                }
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
            String offerTitle = asString(payload.get("title"));
            String label = offerTitle != null ? " \"" + offerTitle + "\"" : "";

            switch (type) {
                case "custom-offer.sent" -> {
                    // Sender is the actor.
                    notifyPerspective(senderId, senderId, NotificationType.CUSTOM_OFFER,
                        "Ponuda poslana",
                        "Ponuda" + label + " je poslana.",
                        null, null, offerId);
                    notifyPerspective(receiverId, senderId, NotificationType.CUSTOM_OFFER,
                        null, null,
                        "Nova prilagođena ponuda",
                        "Primili ste novu ponudu" + label + ".",
                        offerId);
                }
                case "custom-offer.accepted" -> {
                    // Receiver is the actor (they accepted).
                    notifyPerspective(receiverId, receiverId, NotificationType.CUSTOM_OFFER,
                        "Prihvatili ste ponudu",
                        "Ponuda" + label + " je sada ACCEPTED.",
                        null, null, offerId);
                    notifyPerspective(senderId, receiverId, NotificationType.CUSTOM_OFFER,
                        null, null,
                        "Ponuda prihvaćena",
                        "Vaša ponuda" + label + " je sada ACCEPTED.",
                        offerId);
                }
                case "custom-offer.rejected" -> {
                    notifyPerspective(receiverId, receiverId, NotificationType.CUSTOM_OFFER,
                        "Odbili ste ponudu",
                        "Ponuda" + label + " je odbijena.",
                        null, null, offerId);
                    notifyPerspective(senderId, receiverId, NotificationType.CUSTOM_OFFER,
                        null, null,
                        "Ponuda odbijena",
                        "Vaša ponuda" + label + " je odbijena.",
                        offerId);
                }
                case "custom-offer.withdrawn" -> {
                    // Sender is the actor.
                    notifyPerspective(senderId, senderId, NotificationType.CUSTOM_OFFER,
                        "Povukli ste ponudu",
                        "Ponuda" + label + " je povučena.",
                        null, null, offerId);
                    notifyPerspective(receiverId, senderId, NotificationType.CUSTOM_OFFER,
                        null, null,
                        "Ponuda povučena",
                        "Pošiljatelj je povukao ponudu" + label + ".",
                        offerId);
                }
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
            Integer sellerId = asInteger(payload.get("sellerId"));
            Integer version = asInteger(payload.get("versionNumber"));

            // Seller is always the actor for a delivery.
            notifyPerspective(sellerId, sellerId, NotificationType.ORDER_UPDATE,
                "Isporuka poslana (#" + orderId + ")",
                "Verzija " + version + " je poslana klijentu na pregled.",
                null, null, orderId);
            notifyPerspective(clientId, sellerId, NotificationType.ORDER_UPDATE,
                null, null,
                "Nova isporuka (#" + orderId + ")",
                "Verzija " + version + " je isporučena. Prihvatite ili tražite reviziju.",
                orderId);

            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("Failed to handle delivery event: {}", ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * Creates a notification for {@code userId} with the perspective text that
     * matches their role in the event (actor vs counterpart). Passing
     * {@code null} for one perspective's title/content means that party never
     * appears in that role for this event type and the notification is
     * skipped.
     */
    private void notifyPerspective(Integer userId, Integer actorId, NotificationType type,
                                   String actorTitle, String actorContent,
                                   String counterpartTitle, String counterpartContent,
                                   Integer referenceId) {
        if (userId == null) return;
        boolean isActor = Objects.equals(userId, actorId);
        String title = isActor ? actorTitle : counterpartTitle;
        String content = isActor ? actorContent : counterpartContent;
        if (title == null || content == null) return;
        notificationService.create(userId, type, title, content, referenceId);
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
