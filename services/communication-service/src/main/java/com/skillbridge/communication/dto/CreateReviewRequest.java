package com.skillbridge.communication.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
    @NotNull
    @Positive
    Integer orderId,

    @NotNull
    @Positive
    Integer revieweeId,

    @Min(1)
    @Max(5)
    int rating,

    @Size(max = 1000)
    String comment
) {
}
