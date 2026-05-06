package com.skillbridge.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class BatchCreateOrderRequest {

    @NotEmpty(message = "Order list cannot be empty")
    @Size(max = 20, message = "Maximum 20 orders at once")
    @Valid
    private List<CreateOrderRequest> orders;

    public List<CreateOrderRequest> getOrders() { return orders; }
    public void setOrders(List<CreateOrderRequest> orders) { this.orders = orders; }
}