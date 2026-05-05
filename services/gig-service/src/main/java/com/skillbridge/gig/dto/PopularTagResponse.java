package com.skillbridge.gig.dto;

public record PopularTagResponse(
    Integer id,
    String name,
    String slug,
    Long gigCount
) {
}
