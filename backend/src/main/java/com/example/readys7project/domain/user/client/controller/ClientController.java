package com.example.readys7project.domain.user.client.controller;


import com.example.readys7project.domain.user.client.dto.response.ClientsResponseDto;
import com.example.readys7project.domain.user.client.dto.request.UpdateClientProfileRequestDto;
import com.example.readys7project.domain.user.client.dto.response.*;
import com.example.readys7project.domain.user.client.service.ClientService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.dto.PageResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    // 클라이언트 목록 조회
    @GetMapping("/v1/clients")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ClientsResponseDto>>> getClients(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size

            ) {
            Pageable pageable = PageRequest.of(page, size);

        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, clientService.getClients(customUserDetails, pageable)));
    }

    // 클라이언트 상세 조회
    @GetMapping("/v1/clients/{clientId}")
    public ResponseEntity<ApiResponseDto<ClientsResponseDto>> getClientDetail(
            @PathVariable Long clientId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, clientService.getClientDetail(clientId, customUserDetails)));
    }

    // 클라이언트 내 프로필 수정
    @PatchMapping("/v1/clients/{clientId}")
    public ResponseEntity<ApiResponseDto<UpdateClientProfileResponseDto>> updateClientProfile(
            @PathVariable Long clientId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateClientProfileRequestDto updateClientProfileRequestDto
    ){
        return ResponseEntity.ok(ApiResponseDto
                .success(HttpStatus.OK, clientService.updateClientProfile(clientId, customUserDetails, updateClientProfileRequestDto)));
    }

    // 내 프로젝트 목록 조회
    @GetMapping("/v1/clients/my-projects")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ClientProjectsListResponseDto>>> getMyProjects(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, clientService.getMyProjects(customUserDetails, pageable)));
    }
}
