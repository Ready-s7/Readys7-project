package com.example.readys7project.domain.user.auth.controller;

import com.example.readys7project.domain.user.auth.dto.request.UpdateUserInformationRequestDto;
import com.example.readys7project.domain.user.auth.dto.response.GetUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.dto.response.UpdateUserInformationResponseDto;
import com.example.readys7project.domain.user.auth.service.UserService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/v1/users/me")
    public ResponseEntity<ApiResponseDto<GetUserInformationResponseDto>> getUserInformation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, userService.getUserInformation(customUserDetails)));
    }

    @PutMapping("/v1/users/me")
    public ResponseEntity<ApiResponseDto<UpdateUserInformationResponseDto>> updateUserInformation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateUserInformationRequestDto updateUserInformationRequestDto
    ) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, userService.updateUserInformation(
                        customUserDetails,
                        updateUserInformationRequestDto)));
    }

    @DeleteMapping("/v1/users/me")
    public ResponseEntity<ApiResponseDto<Void>> deleteUser(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        userService.deleteUser(customUserDetails);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponseDto.successWithNoContent());
    }
}
