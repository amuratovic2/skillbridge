package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> findAll(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "12") int limit
    ) {
        var result = userService.findAll(page, limit);
        return ApiResponse.ok(result.get("data"), result.get("meta"));
    }

    @GetMapping("/me")
    public ApiResponse<?> getMe(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(userService.findById(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable Integer id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }

    @PatchMapping("/me")
    public ApiResponse<?> update(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody Map<String, String> body
    ) {
        return ApiResponse.ok(userService.update(userId, body));
    }

    @DeleteMapping("/me")
    public ApiResponse<?> deactivate(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(userService.deactivate(userId));
    }
}
