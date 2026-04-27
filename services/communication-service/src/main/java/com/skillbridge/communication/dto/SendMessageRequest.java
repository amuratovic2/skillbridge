package com.skillbridge.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    @NotNull
    @Positive
    Integer receiverId,

    @Positive
    Integer orderId,

    @NotBlank
    @Size(max = 2000)
    String content
) {
}
