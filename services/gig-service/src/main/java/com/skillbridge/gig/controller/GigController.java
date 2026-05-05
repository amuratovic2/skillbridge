package com.skillbridge.gig.controller;

import com.skillbridge.gig.dto.ApiResponse;
import com.skillbridge.gig.dto.CreateGigRequest;
import com.skillbridge.gig.dto.GigResponse;
import com.skillbridge.gig.dto.UpdateGigRequest;
import com.skillbridge.gig.service.GigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/gigs")
@Validated
public class GigController {

    private final GigService gigService;

    public GigController(GigService gigService) {
        this.gigService = gigService;
    }

    @PostMapping
    public ApiResponse<GigResponse> create(
            @RequestHeader("x-user-id") @Positive Integer userId,
            @Valid @RequestBody CreateGigRequest body) {
        GigResponse result = gigService.create(userId, body);
        return ApiResponse.ok(result);
    }

    @GetMapping("/search")
    public ApiResponse<?> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer deliveryTime,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(100) int limit) {
        var result = gigService.search(q, categoryId, minPrice, maxPrice, deliveryTime, sortBy, page, limit);
        return ApiResponse.ok(result.data(), result.meta());
    }

    @GetMapping("/featured")
    public ApiResponse<List<GigResponse>> getFeatured(@RequestParam(defaultValue = "6") @Min(1) @Max(50) int limit) {
        List<GigResponse> result = gigService.getFeatured(limit);
        return ApiResponse.ok(result);
    }

    @GetMapping("/freelancer/{freelancerId}")
    public ApiResponse<List<GigResponse>> findByFreelancer(@PathVariable @Positive Integer freelancerId) {
        List<GigResponse> result = gigService.findByFreelancerId(freelancerId);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<GigResponse> findById(@PathVariable @Positive Integer id) {
        GigResponse result = gigService.findById(id);
        return ApiResponse.ok(result);
    }

    @PatchMapping("/{id}")
    public ApiResponse<GigResponse> update(
            @PathVariable @Positive Integer id,
            @RequestHeader("x-user-id") @Positive Integer userId,
            @Valid @RequestBody UpdateGigRequest body) {
        GigResponse result = gigService.update(id, userId, body);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
            @PathVariable @Positive Integer id,
            @RequestHeader("x-user-id") @Positive Integer userId) {
        var result = gigService.delete(id, userId);
        return ApiResponse.ok(result);
    }
}
