package com.skillbridge.user.controller;

import com.skillbridge.user.dto.ApiResponse;
import com.skillbridge.user.dto.LoginRequest;
import com.skillbridge.user.dto.RefreshTokenRequest;
import com.skillbridge.user.dto.RegisterRequest;
import com.skillbridge.user.dto.TokenRequest;
import com.skillbridge.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.logout(request.refreshToken()));
    }

    @PostMapping("/validate")
    public ApiResponse<?> validate(@Valid @RequestBody TokenRequest request) {
        return ApiResponse.ok(authService.validateToken(request.token()));
    }
}
