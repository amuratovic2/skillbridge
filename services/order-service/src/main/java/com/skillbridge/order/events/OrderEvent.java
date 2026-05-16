package com.skillbridge.order.events;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lifecycle event for an order. Published whenever the order's status changes
 * or a delivery is submitted. Subscribers (e.g. communication-service) translate
 * each event into a notification for the affected user.
 */
public record OrderEvent(
    String eventType,
    Long orderId,
    Integer clientId,
    Integer sellerId,
    Integer gigId,
    String oldStatus,
    String newStatus,
    BigDecimal totalCost,
    Integer actorId,
    String note,
    LocalDateTime occurredAt
) implements Serializable {

    public static OrderEvent of(String type, Long orderId, Integer clientId, Integer sellerId,
                                Integer gigId, String oldStatus, String newStatus,
                                BigDecimal totalCost, Integer actorId, String note) {
        return new OrderEvent(type, orderId, clientId, sellerId, gigId,
            oldStatus, newStatus, totalCost, actorId, note, LocalDateTime.now());
    }
}
