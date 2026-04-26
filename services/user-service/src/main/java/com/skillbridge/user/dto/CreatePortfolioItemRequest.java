package com.skillbridge.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePortfolioItemRequest(
    @NotBlank
    @Size(max = 255)
    String title,

    @Size(max = 2000)
    String description,

    @Size(max = 500)
    String imageUrl
) {
}
