package com.skillbridge.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @NotNull(message = "gigId is required")
    private Integer gigId;

    @NotNull(message = "totalCost is required")
    @Positive(message = "totalCost must be positive")
    private BigDecimal totalCost;

    @Min(value = 1, message = "maxRevisions must be at least 1")
    private int maxRevisions;

    @Min(value = 1, message = "deliveryDays must be at least 1")
    private int deliveryDays;

    public Integer getGigId() { return gigId; }
    public void setGigId(Integer gigId) { this.gigId = gigId; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public int getMaxRevisions() { return maxRevisions; }
    public void setMaxRevisions(int maxRevisions) { this.maxRevisions = maxRevisions; }
    public int getDeliveryDays() { return deliveryDays; }
    public void setDeliveryDays(int deliveryDays) { this.deliveryDays = deliveryDays; }
}