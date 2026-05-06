package com.skillbridge.order.controller;
import com.skillbridge.order.dto.RevisionRequest;

import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.BatchCreateOrderRequest;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.OrderStatusUpdateRequest;
import com.skillbridge.order.mapper.OrderMapper;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.service.OrderPatchService;
import com.skillbridge.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderPatchService orderPatchService;

    public OrderController(OrderService orderService, OrderPatchService orderPatchService) {
        this.orderService = orderService;
        this.orderPatchService = orderPatchService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        return ApiResponse.ok(OrderMapper.toDTO(
            orderService.create(userId, request.getGigId())
        ));
    }

    @PostMapping("/batch")
    public ApiResponse<?> batchCreate(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody BatchCreateOrderRequest request
    ) {
        var orders = orderService.batchCreate(userId, request.getOrders());
        return ApiResponse.ok(orders.stream().map(OrderMapper::toDTO).toList());
    }

    @GetMapping("/my/buying")
    public ApiResponse<?> getMyBuyingOrders(
        @RequestHeader("x-user-id") Integer userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
        @RequestParam(defaultValue = "orderDate") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        var result = orderService.findByClient(userId, page, limit, sortBy, sortDir);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/my/selling")
    public ApiResponse<?> getMySellingOrders(
        @RequestHeader("x-user-id") Integer userId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
        @RequestParam(defaultValue = "orderDate") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        var result = orderService.findBySeller(userId, page, limit, sortBy, sortDir);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable Long id) {
        return ApiResponse.ok(OrderMapper.toDTO(orderService.findById(id)));
    }

    @GetMapping("/my/buying/status/{status}")
    public ApiResponse<?> findByStatus(
        @RequestHeader("x-user-id") Integer userId,
        @PathVariable String status
    ) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        var orders = orderService.findByClientAndStatus(userId, orderStatus);
        return ApiResponse.ok(orders.stream().map(OrderMapper::toDTO).toList());
    }

    @GetMapping("/overdue")
    public ApiResponse<?> findOverdue() {
        var orders = orderService.findOverdue();
        return ApiResponse.ok(orders.stream().map(OrderMapper::toDTO).toList());
    }

    @GetMapping("/statistics/by-status")
    public ApiResponse<?> getStatusStatistics() {
        return ApiResponse.ok(orderService.getStatusStatistics());
    }

    @GetMapping("/my/revenue")
    public ApiResponse<?> getMyRevenue(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(orderService.getTotalRevenue(userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        return ApiResponse.ok(OrderMapper.toDTO(
            orderService.updateStatus(id, userId, newStatus, request.getNote())
        ));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ApiResponse<?> patch(
        @PathVariable Long id,
        @RequestBody JsonPatch patch
    ) {
        return ApiResponse.ok(OrderMapper.toDTO(orderPatchService.patch(id, patch)));
    }

    @PostMapping("/{id}/revision")
    public ApiResponse<?> requestRevision(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody(required = false) RevisionRequest body
    ) {
        String message = body != null ? body.getMessage() : null;
        return ApiResponse.ok(OrderMapper.toDTO(orderService.requestRevision(id, userId, message)));
    }
}