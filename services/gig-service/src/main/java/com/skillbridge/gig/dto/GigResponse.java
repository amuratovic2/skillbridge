package com.skillbridge.gig.dto;

import com.skillbridge.gig.model.GigStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GigResponse(
    Integer id,
    Integer freelancerId,
    String freelancerName,
    String title,
    String description,
    BigDecimal cost,
    Integer deliveryTime,
    Integer revisionCount,
    GigStatus status,
    String coverImage,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    CategoryResponse category,
    List<TagResponse> tags,
    List<GigImageResponse> images
) {
}
