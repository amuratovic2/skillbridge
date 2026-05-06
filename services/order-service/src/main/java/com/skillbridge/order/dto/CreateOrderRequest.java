package com.skillbridge.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "gigId is required")
    private Integer gigId;

    public Integer getGigId() { return gigId; }
    public void setGigId(Integer gigId) { this.gigId = gigId; }
}
