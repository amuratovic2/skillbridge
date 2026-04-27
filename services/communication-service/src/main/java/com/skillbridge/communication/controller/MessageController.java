package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.dto.SendMessageRequest;
import com.skillbridge.communication.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/messages")
@Validated
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ApiResponse<?> send(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @Valid @RequestBody SendMessageRequest request
    ) {
        return ApiResponse.ok(messageService.send(userId, request));
    }

    @GetMapping("/conversations")
    public ApiResponse<?> getConversations(@RequestHeader("x-user-id") @Positive Integer userId) {
        return ApiResponse.ok(messageService.getConversationList(userId));
    }

    @GetMapping("/conversation/{otherUserId}")
    public ApiResponse<?> getConversation(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @PathVariable @Positive Integer otherUserId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        var result = messageService.getConversation(userId, otherUserId, page, limit);
        return ApiResponse.ok(result.data(), result.meta());
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> getByOrder(
        @PathVariable @Positive Integer orderId,
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        var result = messageService.getConversationsByOrder(orderId, page, limit);
        return ApiResponse.ok(result.data(), result.meta());
    }

    @PatchMapping("/read/{senderId}")
    public ApiResponse<?> markAsRead(
        @RequestHeader("x-user-id") @Positive Integer userId,
        @PathVariable @Positive Integer senderId
    ) {
        int updated = messageService.markAsRead(userId, senderId);
        return ApiResponse.ok(Map.of("updated", updated));
    }
}
