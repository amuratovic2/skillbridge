package com.skillbridge.order.mapper;

import com.skillbridge.order.dto.OrderResponse;
import com.skillbridge.order.model.Order;

public class OrderMapper {

    public static OrderResponse toDTO(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.setId(order.getId());
        dto.setClientId(order.getClientId());
        dto.setSellerId(order.getSellerId());
        dto.setGigId(order.getGigId());
        dto.setTotalCost(order.getTotalCost());
        dto.setRequirements(order.getRequirements());
        dto.setStatus(order.getStatus().name());
        dto.setOrderDate(order.getOrderDate());
        dto.setDeliveryDeadline(order.getDeliveryDeadline());
        dto.setMaxRevisions(order.getMaxRevisions());
        dto.setUsedRevisions(order.getUsedRevisions());
        dto.setCompletedAt(order.getCompletedAt());
        dto.setCancelledAt(order.getCancelledAt());
        return dto;
    }

    private OrderMapper() {}
}
