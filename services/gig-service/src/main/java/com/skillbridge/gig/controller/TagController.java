package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.dto.PopularTagResponse;
import com.skillbridge.gig.dto.TagResponse;
import com.skillbridge.gig.service.TagService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tags")
@Validated
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<TagResponse>> findAll() {
        return ApiResponse.ok(tagService.findAll());
    }

    @GetMapping("/popular")
    public ApiResponse<List<PopularTagResponse>> findPopular(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return ApiResponse.ok(tagService.findPopular(limit));
    }
}
