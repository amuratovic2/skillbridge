package com.skillbridge.user.dto;

import java.time.LocalDateTime;

public record PortfolioItemResponse(
    Integer id,
    Integer userId,
    String title,
    String description,
    String imageUrl,
    LocalDateTime createdAt
) {
}
