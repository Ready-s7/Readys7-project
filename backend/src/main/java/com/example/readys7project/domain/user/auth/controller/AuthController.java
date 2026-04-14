package com.example.readys7project.domain.user.auth.controller;

import com.example.readys7project.domain.user.auth.dto.UserDto;
import com.example.readys7project.domain.user.auth.dto.request.AdminRegisterRequestDto;
import com.example.readys7project.domain.user.auth.dto.request.DeveloperRegisterRequestDto;
import com.example.readys7project.domain.user.auth.service.AuthService;
import com.example.readys7project.domain.user.auth.dto.request.ClientRegisterRequestDto;
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
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 클라이언트 회원가입
    @PostMapping("/v1/auth/register/clients")
    public ResponseEntity<ApiResponseDto<UserDto>> registerClient(
            @Valid @RequestBody ClientRegisterRequestDto clientRegisterRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success
                (HttpStatus.CREATED, authService.registerClient(
                        clientRegisterRequestDto)));
    }

    // 개발자 회원가입
    @PostMapping("/v1/auth/register/developers")
    public ResponseEntity<ApiResponseDto<UserDto>> registerDeveloper(
            @Valid @RequestBody DeveloperRegisterRequestDto developerRegisterRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success
                (HttpStatus.CREATED, authService.registerDeveloper(
                        developerRegisterRequestDto)));
    }

    // 관리자 회원가입
    @PostMapping("/v1/auth/register/admins")
    public ResponseEntity<ApiResponseDto<UserDto>> registerAdmin(
            @Valid @RequestBody AdminRegisterRequestDto adminRegisterRequestDto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponseDto.success
                (HttpStatus.CREATED, authService.registerAdmin(
                        adminRegisterRequestDto)));
    }

    /**
     * 로그인 API
     * POST /api/auth/login
     * - Access Token → Response Header (Authorization: Bearer {token})
     * - Refresh Token → Response Body
     */
    @PostMapping("/v1/auth/login")
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
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
