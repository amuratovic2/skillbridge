package com.skillbridge.user.controller;

import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.dto.UpdateUserRequest;
import com.skillbridge.user.model.UserRole;
import com.skillbridge.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ApiResponse<?> findAll(
        @RequestParam(defaultValue = "1") @Min(1) int page,
        @RequestParam(defaultValue = "12") @Min(1) @Max(100) int limit,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) UserRole role,
        @RequestParam(required = false) String country,
        @RequestParam(required = false) String skill
    ) {
        var result = userService.findAll(page, limit, sortBy, sortDirection, query, role, country, skill);
        return ApiResponse.ok(result.data(), result.meta());
    }

    @GetMapping("/me")
    public ApiResponse<?> getMe(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(userService.findById(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> findById(@PathVariable Integer id) {
        return ApiResponse.ok(userService.getPublicProfile(id));
    }

    @PatchMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<?> update(
        @RequestHeader("x-user-id") Integer userId,
        @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.ok(userService.update(userId, request));
    }

    @PatchMapping(value = "/me", consumes = "application/json-patch+json")
    public ApiResponse<?> patch(
        @RequestHeader("x-user-id") Integer userId,
        @RequestBody JsonPatch patch
    ) {
        return ApiResponse.ok(userService.patch(userId, patch));
    }

    @DeleteMapping("/me")
    public ApiResponse<?> deactivate(@RequestHeader("x-user-id") Integer userId) {
        return ApiResponse.ok(userService.deactivate(userId));
    }
}
