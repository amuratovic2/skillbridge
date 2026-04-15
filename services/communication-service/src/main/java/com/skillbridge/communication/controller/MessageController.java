package com.skillbridge.communication.controller;

import com.skillbridge.communication.dto.ApiResponse;
import com.skillbridge.communication.service.MessageService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ApiResponse<?> send(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, Object> body
    ) {
        Integer receiverId = (Integer) body.get("receiverId");
        Integer orderId = body.containsKey("orderId") ? (Integer) body.get("orderId") : null;
        String content = (String) body.get("content");
        return ApiResponse.ok(messageService.send(userId, receiverId, orderId, content));
    }

    @GetMapping("/conversations")
    public ApiResponse<?> getConversations(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(messageService.getConversationList(userId));
    }

    @GetMapping("/conversation/{otherUserId}")
    public ApiResponse<?> getConversation(
        @RequestHeader("x-user-id") Integer userId,
        @PathVariable Integer otherUserId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit
    ) {
        var result = messageService.getConversation(userId, otherUserId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/order/{orderId}")
    public ApiResponse<?> getByOrder(
        @PathVariable Integer orderId,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int limit
    ) {
        var result = messageService.getConversationsByOrder(orderId, page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @PatchMapping("/read/{senderId}")
    public ApiResponse<?> markAsRead(
        @RequestHeader("x-user-id") Integer userId,
        @PathVariable Integer senderId
    ) {
        int updated = messageService.markAsRead(userId, senderId);
        return ApiResponse.ok(Map.of("updated", updated));
    }
}
