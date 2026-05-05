package com.skillbridge.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplaceUserSkillsRequest(
    @NotEmpty
    @Size(max = 50)
    List<@NotNull Integer> skillIds
) {
}
