package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.dto.CreateCategoryRequest;
import com.skillbridge.gig.model.Category;
import com.skillbridge.gig.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<Category>> findAll() {
        return ApiResponse.ok(categoryService.findAll());
    }

    @GetMapping("/{slug}")
    public ApiResponse<Category> findBySlug(@PathVariable String slug) {
        return ApiResponse.ok(categoryService.findBySlug(slug));
    }

    @PostMapping
    public ApiResponse<Category> create(@RequestBody CreateCategoryRequest body) {
        return ApiResponse.ok(categoryService.create(body.getTitle()));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Category> update(@PathVariable Integer id, @RequestBody CreateCategoryRequest body) {
        return ApiResponse.ok(categoryService.update(id, body.getTitle()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Integer id) {
        return ApiResponse.ok(categoryService.delete(id));
    }
}
