package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.service.PortfolioService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        @RequestBody Map<String, String> body
    ) {
        return ApiResponse.ok(portfolioService.create(userId, body));
    }

    @PatchMapping("/{id}")
    public ApiResponse<?> update(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        return ApiResponse.ok(portfolioService.update(id, userId, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer userId
    ) {
        return ApiResponse.ok(portfolioService.delete(id, userId));
    }
}
