package com.skillbridge.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderPatchState {

    private BigDecimal totalCost;
    private Integer maxRevisions;

    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public Integer getMaxRevisions() { return maxRevisions; }
    public void setMaxRevisions(Integer maxRevisions) { this.maxRevisions = maxRevisions; }
}