package com.skillbridge.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateDisputeRequest(
    @NotNull
    @Positive
    Integer orderId,

    @NotBlank
    @Size(max = 255)
    String reason,

    @NotBlank
    @Size(max = 4000)
    String description
) {
}
