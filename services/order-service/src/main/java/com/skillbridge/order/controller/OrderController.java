package com.skillbridge.order.controller;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, Object> body
    ) {
        Integer gigId = (Integer) body.get("gigId");
        BigDecimal totalCost = new BigDecimal(body.get("totalCost").toString());
        int maxRevisions = (Integer) body.get("maxRevisions");
        int deliveryDays = (Integer) body.get("deliveryDays");
        return ApiResponse.ok(orderService.create(userId, gigId, totalCost, maxRevisions, deliveryDays));
    }

    @GetMapping("/my/buying")
    public ApiResponse<?> getMyBuyingOrders(
        @RequestHeader("x-user-id") Integer userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var result = orderService.findByClient(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/my/selling")
    public ApiResponse<?> getMySellingOrders(
        @RequestHeader("x-user-id") Integer userId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var result = orderService.findBySeller(userId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable Long id) {
        return ApiResponse.ok(orderService.findById(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        OrderStatus newStatus = OrderStatus.valueOf(body.get("status"));
        String note = body.get("note");
        return ApiResponse.ok(orderService.updateStatus(id, userId, newStatus, note));
    }

    @PostMapping("/{id}/revision")
    public ApiResponse<?> requestRevision(
        @PathVariable Long id,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        return ApiResponse.ok(orderService.requestRevision(id, userId, body.get("message")));
    }
}
