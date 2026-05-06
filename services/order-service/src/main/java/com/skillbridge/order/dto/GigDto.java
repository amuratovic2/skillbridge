package com.skillbridge.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Mirrors the relevant fields of gig-service's GigResponse.
 * Unknown fields (title, images, tags, etc.) are ignored during deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GigDto {

    private Integer id;
    private Integer freelancerId;
    private BigDecimal cost;
    private Integer deliveryTime;
    private Integer revisionCount;
    private String status;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getFreelancerId() { return freelancerId; }
    public void setFreelancerId(Integer freelancerId) { this.freelancerId = freelancerId; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public Integer getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(Integer deliveryTime) { this.deliveryTime = deliveryTime; }

    public Integer getRevisionCount() { return revisionCount; }
    public void setRevisionCount(Integer revisionCount) { this.revisionCount = revisionCount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
