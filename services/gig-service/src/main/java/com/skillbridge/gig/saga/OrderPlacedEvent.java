package com.skillbridge.gig.saga;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderPlacedEvent(
    Long orderId,
    Integer gigId,
    Integer clientId,
    Integer sellerId,
    BigDecimal totalCost
) implements Serializable {}
