package com.skillbridge.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderResponse {

    private Long id;
    private Integer clientId;
    private Integer gigId;
    private BigDecimal totalCost;
    private String status;
    private LocalDateTime orderDate;
    private LocalDateTime deliveryDeadline;
    private int maxRevisions;
    private int usedRevisions;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getClientId() { return clientId; }
    public void setClientId(Integer clientId) { this.clientId = clientId; }
    public Integer getGigId() { return gigId; }
    public void setGigId(Integer gigId) { this.gigId = gigId; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public LocalDateTime getDeliveryDeadline() { return deliveryDeadline; }
    public void setDeliveryDeadline(LocalDateTime deliveryDeadline) { this.deliveryDeadline = deliveryDeadline; }
    public int getMaxRevisions() { return maxRevisions; }
    public void setMaxRevisions(int maxRevisions) { this.maxRevisions = maxRevisions; }
    public int getUsedRevisions() { return usedRevisions; }
    public void setUsedRevisions(int usedRevisions) { this.usedRevisions = usedRevisions; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}