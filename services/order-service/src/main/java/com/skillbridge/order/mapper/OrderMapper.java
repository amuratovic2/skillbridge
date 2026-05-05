package com.skillbridge.order.mapper;

import com.skillbridge.order.dto.OrderResponse;
import com.skillbridge.order.model.Order;

public class OrderMapper {

    public static OrderResponse toDTO(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setId(order.getId());
        dto.setClientId(order.getClientId());
        dto.setGigId(order.getGigId());
        dto.setTotalCost(order.getTotalCost());
        dto.setStatus(order.getStatus().name());
        return dto;
    }
}