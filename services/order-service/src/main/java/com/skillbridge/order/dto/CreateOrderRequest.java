package com.skillbridge.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateOrderRequest {

    @NotNull
    private Integer gigId;

    @NotNull
    @Positive
    private BigDecimal totalCost;

    @Min(1)
    private int maxRevisions;

    @Min(1)
    private int deliveryDays;

    public Integer getGigId() {
        return gigId;
    }

    public void setGigId(Integer gigId) {
        this.gigId = gigId;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public int getMaxRevisions() {
        return maxRevisions;
    }

    public void setMaxRevisions(int maxRevisions) {
        this.maxRevisions = maxRevisions;
    }

    public int getDeliveryDays() {
        return deliveryDays;
    }

    public void setDeliveryDays(int deliveryDays) {
        this.deliveryDays = deliveryDays;
    }
}