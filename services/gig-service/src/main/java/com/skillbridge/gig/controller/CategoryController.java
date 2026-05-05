package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.dto.CategoryResponse;
import com.skillbridge.gig.dto.CreateCategoryRequest;
import com.skillbridge.gig.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> findAll() {
        return ApiResponse.ok(categoryService.findAll());
    }

    @GetMapping("/{slug}")
    public ApiResponse<CategoryResponse> findBySlug(@PathVariable String slug) {
        return ApiResponse.ok(categoryService.findBySlug(slug));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest body) {
        return ApiResponse.ok(categoryService.create(body.getTitle()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @PathVariable @Positive Integer id,
            @Valid @RequestBody CreateCategoryRequest body) {
        return ApiResponse.ok(categoryService.update(id, body.getTitle()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable @Positive Integer id) {
        return ApiResponse.ok(categoryService.delete(id));
    }
}
