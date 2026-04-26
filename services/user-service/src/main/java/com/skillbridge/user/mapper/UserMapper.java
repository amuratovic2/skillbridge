package com.skillbridge.user.mapper;

import com.skillbridge.user.dto.PortfolioItemResponse;
import com.skillbridge.user.dto.SkillResponse;
import com.skillbridge.user.dto.UserResponse;
import com.skillbridge.user.model.PortfolioItem;
import com.skillbridge.user.model.Skill;
import com.skillbridge.user.model.User;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toSummary(User user) {
        return toResponse(user, false);
    }

    public static UserResponse toResponse(User user) {
        return toResponse(user, true);
    }

    public static SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName());
    }

    public static PortfolioItemResponse toResponse(PortfolioItem item) {
        return new PortfolioItemResponse(
            item.getId(),
            item.getUserId(),
            item.getTitle(),
            item.getDescription(),
            item.getImageUrl(),
            item.getCreatedAt()
        );
    }

    private static UserResponse toResponse(User user, boolean includeRelations) {
        List<SkillResponse> skills = includeRelations
            ? user.getSkills().stream().map(UserMapper::toResponse).toList()
            : null;

        List<PortfolioItemResponse> portfolioItems = includeRelations
            ? user.getPortfolioItems().stream().map(UserMapper::toResponse).toList()
            : null;

        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getRole(),
            user.getFirstName(),
            user.getLastName(),
            user.getBio(),
            user.getProfilePicture(),
            user.getCountry(),
            user.getIsActive(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            skills,
            portfolioItems
        );
    }
}
