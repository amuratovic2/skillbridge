package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.dto.CreateDisputeRequest;
import com.skillbridge.communication.dto.ResolveDisputeRequest;
import com.skillbridge.communication.model.DisputeStatus;
import com.skillbridge.communication.service.DisputeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/disputes")
@Validated
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @Valid @RequestBody CreateDisputeRequest request
    ) {
        return ApiResponse.ok(disputeService.create(
            userId,
            request.orderId(),
            request.reason(),
            request.description()
        ));
    }

    @GetMapping
    public ApiResponse<?> findAll(
        @RequestParam(required = false) DisputeStatus status,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        var result = disputeService.findAll(status, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable @Positive Integer id) {
        return ApiResponse.ok(disputeService.findById(id));
    }

    @PatchMapping("/{id}/assign")
    public ApiResponse<?> assign(
        @PathVariable @Positive Integer id,
        @RequestHeader("x-user-id") @Positive Integer adminId
    ) {
        return ApiResponse.ok(disputeService.assign(id, adminId));
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<?> resolve(
        @PathVariable @Positive Integer id,
        @RequestHeader("x-user-id") @Positive Integer adminId,
        @Valid @RequestBody ResolveDisputeRequest request
    ) {
        return ApiResponse.ok(disputeService.resolve(id, adminId, request.resolution(), request.status()));
    }
}
