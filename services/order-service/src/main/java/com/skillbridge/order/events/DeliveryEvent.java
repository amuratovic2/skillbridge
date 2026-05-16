package com.skillbridge.order.events;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Published when a freelancer submits a new delivery version on an order.
 */
public record DeliveryEvent(
    String eventType,
    Long orderId,
    Long deliveryId,
    Integer clientId,
    Integer sellerId,
    int versionNumber,
    String message,
    LocalDateTime occurredAt
) implements Serializable {

    public static DeliveryEvent created(Long orderId, Long deliveryId, Integer clientId,
                                        Integer sellerId, int versionNumber, String message) {
        return new DeliveryEvent("delivery.created", orderId, deliveryId, clientId,
            sellerId, versionNumber, message, LocalDateTime.now());
    }
}
