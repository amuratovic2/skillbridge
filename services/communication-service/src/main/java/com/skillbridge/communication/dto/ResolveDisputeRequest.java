package com.skillbridge.communication.dto;

import com.skillbridge.communication.model.DisputeStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveDisputeRequest(
    @NotBlank
    @Size(max = 4000)
    String resolution,

    @NotNull
    DisputeStatus status
) {
}
