package com.skillbridge.gig.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Integer getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(Integer deliveryTime) { this.deliveryTime = deliveryTime; }

    public Integer getRevisionCount() { return revisionCount; }
    public void setRevisionCount(Integer revisionCount) { this.revisionCount = revisionCount; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
