package com.skillbridge.gig.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CreateGigRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    @Positive
    private Integer categoryId;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal cost;

    @NotNull
    @Positive
    private Integer deliveryTime;

    @NotNull
    @PositiveOrZero
    private Integer revisionCount;

    @Size(max = 500)
    private String coverImage;

    private List<@NotBlank @Size(max = 100) String> tags;
}
