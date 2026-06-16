package com.skillbridge.order.controller;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.BatchCreateOrderRequest;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.OrderStatusUpdateRequest;
import com.skillbridge.order.dto.RevisionRequest;
import com.skillbridge.order.mapper.OrderMapper;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.service.OrderPatchService;
import com.skillbridge.order.service.OrderService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
    public ResponseEntity<ApiResponse<?>> create(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody CreateOrderRequest request
    ) {
        var order = OrderMapper.toDTO(orderService.create(userId, request.getGigId(), request.getRequirements()));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(
            order,
            "Kreiranje narudzbe je zapoceto. Zavrsni rezultat validacije stize kroz notifikaciju."
        ));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<?>> batchCreate(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody BatchCreateOrderRequest request
    ) {
        var orders = orderService.batchCreate(userId, request.getOrders());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.accepted(
            orders.stream().map(OrderMapper::toDTO).toList(),
            "Kreiranje narudzbi je zapoceto. Zavrsni rezultat validacije stize kroz notifikacije."
        ));
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
    public ApiResponse<?> findById(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestHeader("x-user-role") String userRole
    ) {
        return ApiResponse.ok(OrderMapper.toDTO(orderService.findByIdForUser(id, userId, userRole)));
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
        @RequestHeader("x-user-role") String userRole,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        OrderStatus newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        return ApiResponse.ok(OrderMapper.toDTO(
            orderService.updateStatus(id, userId, userRole, newStatus, request.getNote())
        ));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
    public ApiResponse<?> patch(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestHeader("x-user-role") String userRole,
        @RequestBody JsonPatch patch
    ) {
        return ApiResponse.ok(OrderMapper.toDTO(orderPatchService.patch(id, userId, userRole, patch)));
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
