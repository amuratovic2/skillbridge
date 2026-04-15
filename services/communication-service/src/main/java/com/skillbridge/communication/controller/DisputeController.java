package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.model.DisputeStatus;
import com.skillbridge.communication.service.DisputeService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ApiResponse<?> create(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, Object> body
    ) {
        Integer orderId = (Integer) body.get("orderId");
        String reason = (String) body.get("reason");
        String description = (String) body.get("description");
        return ApiResponse.ok(disputeService.create(userId, orderId, reason, description));
    }

    @GetMapping
    public ApiResponse<?> findAll(
        @RequestParam(required = false) DisputeStatus status,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int limit
    ) {
        var result = disputeService.findAll(status, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable Integer id) {
        return ApiResponse.ok(disputeService.findById(id));
    }

    @PatchMapping("/{id}/assign")
    public ApiResponse<?> assign(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer adminId
    ) {
        return ApiResponse.ok(disputeService.assign(id, adminId));
    }

    @PatchMapping("/{id}/resolve")
    public ApiResponse<?> resolve(
        @PathVariable Integer id,
        @RequestHeader("x-user-id") Integer adminId,
        @RequestBody Map<String, Object> body
    ) {
        String resolution = (String) body.get("resolution");
        DisputeStatus status = DisputeStatus.valueOf((String) body.get("status"));
        return ApiResponse.ok(disputeService.resolve(id, adminId, resolution, status));
    }
}
