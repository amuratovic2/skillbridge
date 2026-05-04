package com.skillbridge.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchCreatePortfolioItemsRequest(
    @NotEmpty
    @Size(max = 20)
    List<@NotNull @Valid CreatePortfolioItemRequest> items
) {
}
