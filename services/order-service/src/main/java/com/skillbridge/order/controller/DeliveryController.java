package com.skillbridge.order.controller;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PostMapping("/order/{orderId}")
    public ApiResponse<?> create(
        @PathVariable Long orderId,
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        return ApiResponse.ok(deliveryService.create(
            orderId, userId,
            body.get("message"),
            body.get("fileUrl"),
            body.get("fileName")
        ));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> findByOrderId(@PathVariable Long orderId) {
        return ApiResponse.ok(deliveryService.findByOrderId(orderId));
    }

    @GetMapping("/order/{orderId}/version/{version}")
    public ApiResponse<?> findByVersion(
        @PathVariable Long orderId,
        @PathVariable int version
    ) {
        return ApiResponse.ok(
            deliveryService.findByVersion(orderId, version)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery version not found"))
        );
    }
}
