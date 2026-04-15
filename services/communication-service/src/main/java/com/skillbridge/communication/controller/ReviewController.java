package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.service.ReviewService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, Object> body
    ) {
        Integer orderId = (Integer) body.get("orderId");
        Integer revieweeId = (Integer) body.get("revieweeId");
        int rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");
        return ApiResponse.ok(reviewService.create(userId, orderId, revieweeId, rating, comment));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<?> findByUser(
        @PathVariable Integer userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var result = reviewService.findByReviewee(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> findByOrder(@PathVariable Integer orderId) {
        return ApiResponse.ok(reviewService.findByOrder(orderId));
    }

    @GetMapping("/rating/{userId}")
    public ApiResponse<?> getAverageRating(@PathVariable Integer userId) {
        return ApiResponse.ok(reviewService.getAverageRating(userId));
    }
}
