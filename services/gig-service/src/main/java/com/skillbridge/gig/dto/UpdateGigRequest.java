package com.skillbridge.gig.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class UpdateGigRequest {
    @Size(max = 255)
    private String title;

    @Size(max = 5000)
    private String description;

    @Positive
    private Integer categoryId;

    @DecimalMin(value = "0.01")
    private BigDecimal cost;

    @Positive
    private Integer deliveryTime;

    @PositiveOrZero
    private Integer revisionCount;

    @Size(max = 500)
    private String coverImage;

    @Pattern(regexp = "DRAFT|ACTIVE|PAUSED|DELETED", message = "must be one of DRAFT, ACTIVE, PAUSED, DELETED")
    private String status;

    private List<@NotBlank @Size(max = 100) String> tags;
}
