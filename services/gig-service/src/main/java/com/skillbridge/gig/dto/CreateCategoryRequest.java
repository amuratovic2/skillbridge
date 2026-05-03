package com.skillbridge.gig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCategoryRequest {
    @NotBlank
    @Size(max = 255)
    private String title;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
