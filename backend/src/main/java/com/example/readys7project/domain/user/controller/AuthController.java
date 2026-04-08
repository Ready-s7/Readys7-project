package com.example.readys7project.domain.user.controller;

import com.example.readys7project.domain.user.dto.request.LoginRequest;
import com.example.readys7project.domain.user.dto.request.RegisterRequest;
import com.example.readys7project.domain.user.dto.response.AuthResponse;
import com.example.readys7project.domain.user.service.AuthService;
import com.example.readys7project.global.dto.ApiResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<AuthService.AuthTokenDto>> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, authService.login(request)));
    }
}
