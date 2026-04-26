package com.skillbridge.user.dto;

import com.skillbridge.user.model.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
    Integer id,
    String username,
    String email,
    UserRole role,
    String firstName,
    String lastName,
    String bio,
    String profilePicture,
    String country,
    Boolean isActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<SkillResponse> skills,
    List<PortfolioItemResponse> portfolioItems
) {
}
