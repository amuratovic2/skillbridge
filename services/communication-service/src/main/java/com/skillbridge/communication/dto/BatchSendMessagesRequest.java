package com.skillbridge.communication.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchSendMessagesRequest(
    @NotEmpty
    @Size(max = 100)
    List<@Valid SendMessageRequest> messages
) {
}
