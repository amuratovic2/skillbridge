package com.skillbridge.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.CreateDeliveryRequest;
import com.skillbridge.order.service.DeliveryService;

import jakarta.validation.Valid;

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
        @Valid @RequestBody CreateDeliveryRequest request
    ) {
        return ApiResponse.ok(
            deliveryService.create(
                orderId,
                userId,
                request.getMessage(),
                request.getFileUrl(),
                request.getFileName()
            )
        );
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> findByOrderId(
        @PathVariable Long orderId,
        @RequestHeader("x-user-id") Integer userId,
        @RequestHeader("x-user-role") String userRole
    ) {
        return ApiResponse.ok(deliveryService.findByOrderId(orderId, userId, userRole));
    }

    @GetMapping("/order/{orderId}/version/{version}")
    public ApiResponse<?> findByVersion(
        @PathVariable Long orderId,
        @PathVariable int version,
        @RequestHeader("x-user-id") Integer userId,
        @RequestHeader("x-user-role") String userRole
    ) {
        return ApiResponse.ok(
            deliveryService.findByVersion(orderId, version, userId, userRole)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery version not found"))
        );
    }
}
