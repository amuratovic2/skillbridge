package com.skillbridge.order.dto;

import java.math.BigDecimal;

public class OrderResponse {

    private Long id;
    private Integer clientId;
    private Integer gigId;
    private BigDecimal totalCost;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}