package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.dto.CreatePortfolioItemRequest;
import com.skillbridge.user.dto.UpdatePortfolioItemRequest;
import com.skillbridge.user.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<?> findByUserId(@PathVariable Integer userId) {
        return ApiResponse.ok(portfolioService.findByUserId(userId));
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody CreatePortfolioItemRequest request
    ) {
        return ApiResponse.ok(portfolioService.create(userId, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<?> update(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody UpdatePortfolioItemRequest request
    ) {
        return ApiResponse.ok(portfolioService.update(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(portfolioService.delete(id, userId));
    }
}
