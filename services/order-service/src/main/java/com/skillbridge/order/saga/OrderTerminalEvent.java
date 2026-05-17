package com.skillbridge.order.saga;

import java.io.Serializable;

public record OrderTerminalEvent(
    Long orderId,
    Integer gigId
) implements Serializable {}
