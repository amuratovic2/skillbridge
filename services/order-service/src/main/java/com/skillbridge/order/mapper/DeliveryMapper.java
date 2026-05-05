package com.skillbridge.order.mapper;

import com.skillbridge.order.dto.DeliveryResponse;
import com.skillbridge.order.model.Delivery;

public class DeliveryMapper {

    public static DeliveryResponse toDTO(Delivery delivery) {
        DeliveryResponse dto = new DeliveryResponse();

        dto.setId(delivery.getId());
        dto.setOrderId(delivery.getOrder().getId());
        dto.setVersionNumber(delivery.getVersionNumber());
        dto.setMessage(delivery.getMessage());
        dto.setFileUrl(delivery.getFileUrl());
        dto.setFileName(delivery.getFileName());
        dto.setCreatedAt(delivery.getCreatedAt());

        return dto;
    }
}