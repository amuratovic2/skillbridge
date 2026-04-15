package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.model.Tag;
import com.skillbridge.gig.service.TagService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    public ApiResponse<List<Tag>> findAll() {
        return ApiResponse.ok(tagService.findAll());
    }

    @GetMapping("/popular")
    public ApiResponse<List<Map<String, Object>>> findPopular(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(tagService.findPopular(limit));
    }
}
