package com.skillbridge.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull(message = "gigId is required")
    private Integer gigId;

    @Size(max = 2000, message = "requirements cannot exceed 2000 characters")
    private String requirements;

    public Integer getGigId() { return gigId; }
    public void setGigId(Integer gigId) { this.gigId = gigId; }
    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
}
