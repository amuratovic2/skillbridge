package com.skillbridge.order.mapper;

import com.skillbridge.order.dto.CustomOfferResponse;
import com.skillbridge.order.model.CustomOffer;

public class CustomOfferMapper {

    public static CustomOfferResponse toDTO(CustomOffer offer) {
        CustomOfferResponse dto = new CustomOfferResponse();
        dto.setId(offer.getId());
        dto.setGigId(offer.getGigId());
        dto.setOrderId(offer.getOrderId());
        dto.setSenderId(offer.getSenderId());
        dto.setReceiverId(offer.getReceiverId());
        dto.setTitle(offer.getTitle());
        dto.setDescription(offer.getDescription());
        dto.setPrice(offer.getPrice());
        dto.setDeliveryDays(offer.getDeliveryDays());
        dto.setRevisionCount(offer.getRevisionCount());
        dto.setStatus(offer.getStatus().name());
        dto.setCreatedAt(offer.getCreatedAt());
        dto.setExpiresAt(offer.getExpiresAt());
        return dto;
    }

    private CustomOfferMapper() {}
}
