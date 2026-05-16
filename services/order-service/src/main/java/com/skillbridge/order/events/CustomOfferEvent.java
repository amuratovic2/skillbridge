package com.skillbridge.order.events;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lifecycle event for a custom offer. Published when an offer is sent,
 * accepted, rejected or withdrawn.
 */
public record CustomOfferEvent(
    String eventType,
    Long offerId,
    Integer senderId,
    Integer receiverId,
    Integer gigId,
    String title,
    BigDecimal price,
    String status,
    LocalDateTime occurredAt
) implements Serializable {

    public static CustomOfferEvent of(String type, Long offerId, Integer senderId,
                                      Integer receiverId, Integer gigId,
                                      String title, BigDecimal price, String status) {
        return new CustomOfferEvent(type, offerId, senderId, receiverId, gigId,
            title, price, status, LocalDateTime.now());
    }
}
