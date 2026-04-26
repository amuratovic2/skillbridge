package com.skillbridge.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSkillRequest(
    @NotBlank
    @Size(max = 100)
    String name
) {
}
