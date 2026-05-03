package com.skillbridge.gig.dto;

public record GigImageResponse(
    Integer id,
    String imageUrl,
    Integer sortOrder
) {
}
