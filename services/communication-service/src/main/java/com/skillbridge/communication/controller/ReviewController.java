package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.dto.CreateReviewRequest;
import com.skillbridge.communication.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @Valid @RequestBody CreateReviewRequest request
    ) {
        return ApiResponse.ok(reviewService.create(
            userId,
            request.orderId(),
            request.revieweeId(),
            request.rating(),
            request.comment()
        ));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<?> findByUser(
        @PathVariable @Positive Integer userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        var result = reviewService.findByReviewee(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> findByOrder(@PathVariable @Positive Integer orderId) {
        return ApiResponse.ok(reviewService.findByOrder(orderId));
    }

    @GetMapping("/rating/{userId}")
    public ApiResponse<?> getAverageRating(@PathVariable @Positive Integer userId) {
        return ApiResponse.ok(reviewService.getAverageRating(userId));
    }
}
