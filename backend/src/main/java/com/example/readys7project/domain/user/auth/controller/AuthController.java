package com.example.readys7project.domain.user.auth.controller;

import com.example.readys7project.domain.user.auth.dto.request.UserRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.AuthResponse;
import com.example.readys7project.domain.user.auth.service.AuthService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.dto.LoginRequestDto;
import com.example.readys7project.global.dto.LoginResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/v1/auth/register")
    public ResponseEntity<ApiResponseDto<AuthResponse>> register(
            @Valid @RequestBody UserRegisterRequestDto userRegisterRequestDto) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, authService.register(userRegisterRequestDto)));
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     * - Access Token → Response Header (Authorization: Bearer {token})
     * - Refresh Token → Response Body
     */
    @PostMapping("/v1/aut/login")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request
    ) {

        AuthService.AuthTokenDto tokens = authService.login(request);

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(tokens.refreshToken())
                                .email(tokens.email())
                                .build()
                ));
    }

    /**
     * 토큰 재발급 API
     * POST /api/auth/reissue
     */
    @PostMapping("/v1/auth/reissue")
    public ResponseEntity<ApiResponseDto<LoginResponseDto>> reissue(
            @RequestBody LoginResponseDto request
    ) {

        AuthService.AuthTokenDto tokens = authService.reissue(request.refreshToken());

        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokens.accessToken())
                .body(ApiResponseDto.success(
                        HttpStatus.OK,
                        LoginResponseDto.builder()
                                .refreshToken(tokens.refreshToken())
                                .email(tokens.email())
                                .build()
                ));
    }

    /**
     * 로그아웃 API
     * POST /api/auth/logout
     */
    @PostMapping("/v1/auth/logout")
    public ResponseEntity<ApiResponseDto<Void>> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        authService.logout(userDetails.getEmail());
        return ResponseEntity.ok()
                .body(ApiResponseDto.successWithNoContent());
    }
}
