package com.skillbridge.gig.saga;

import java.io.Serializable;

public record OrderTerminalEvent(
    Long orderId,
    Integer gigId
) implements Serializable {}
