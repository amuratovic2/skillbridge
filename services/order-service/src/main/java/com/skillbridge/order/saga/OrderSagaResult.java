package com.skillbridge.order.saga;

import java.io.Serializable;

public record OrderSagaResult(
    Long orderId,
    boolean confirmed,
    String reason
) implements Serializable {}
