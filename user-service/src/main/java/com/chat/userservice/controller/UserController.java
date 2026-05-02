package com.chat.userservice.controller;

import com.chat.userservice.dto.request.LoginRequest;
import com.chat.userservice.dto.request.RegisterRequest;
import com.chat.userservice.dto.response.LoginResponse;
import com.chat.userservice.dto.response.RegisterResponse;
import com.chat.userservice.dto.response.UserResponse;
import com.chat.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * POST /api/users/register
     * Public endpoint — no JWT required.
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/users/login
     * Public endpoint — no JWT required.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/me
     * Protected — requires valid JWT (enforced by API Gateway).
     * Reads caller identity from X-User-Id header forwarded by the gateway.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @RequestHeader("X-User-Id") String userId) {
        UserResponse response = userService.getCurrentUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/users/{userId}
     * Protected — requires valid JWT (enforced by API Gateway).
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }
}
