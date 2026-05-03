package com.skillbridge.gig.mapper;

import com.skillbridge.gig.dto.CategoryResponse;
import com.skillbridge.gig.dto.GigImageResponse;
import com.skillbridge.gig.dto.GigResponse;
import com.skillbridge.gig.dto.TagResponse;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.model.Gig;
import com.skillbridge.gig.model.GigImage;
import com.skillbridge.gig.model.Tag;

import java.util.List;

public final class GigMapper {

    private GigMapper() {
    }

    public static GigResponse toResponse(Gig gig) {
        return new GigResponse(
            gig.getId(),
            gig.getFreelancerId(),
            gig.getFreelancerName(),
            gig.getTitle(),
            gig.getDescription(),
            gig.getCost(),
            gig.getDeliveryTime(),
            gig.getRevisionCount(),
            gig.getStatus(),
            gig.getCoverImage(),
            gig.getCreatedAt(),
            gig.getUpdatedAt(),
            toResponse(gig.getCategory()),
            gig.getTags().stream().map(GigMapper::toResponse).toList(),
            gig.getImages().stream().map(GigMapper::toResponse).toList()
        );
    }

    public static CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(category.getId(), category.getTitle(), category.getSlug());
    }

    public static TagResponse toResponse(Tag tag) {
        return new TagResponse(tag.getId(), tag.getName(), tag.getSlug());
    }

    public static GigImageResponse toResponse(GigImage image) {
        return new GigImageResponse(image.getId(), image.getImageUrl(), image.getSortOrder());
    }

    public static List<GigResponse> toResponses(List<Gig> gigs) {
        return gigs.stream().map(GigMapper::toResponse).toList();
    }
}
