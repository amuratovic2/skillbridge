package com.skillbridge.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchCreateSkillsRequest(
    @NotEmpty
    @Size(max = 50)
    List<@NotNull @Valid CreateSkillRequest> skills
) {
}
