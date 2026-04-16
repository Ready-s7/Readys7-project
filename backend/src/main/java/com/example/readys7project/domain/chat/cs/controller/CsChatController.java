package com.example.readys7project.domain.chat.cs.controller;

import com.example.readys7project.domain.chat.cs.dto.CsChatRoomRequestDto;
import com.example.readys7project.domain.chat.cs.dto.CsChatRoomResponseDto;
import com.example.readys7project.domain.chat.cs.dto.CsMessageResponseDto;
import com.example.readys7project.domain.chat.cs.enums.CsStatus;
import com.example.readys7project.domain.chat.cs.service.CsChatService;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CsChatController {

    private final CsChatService csChatService;

    // 사용자/관리자: CS 채팅방 상세 조회
    @GetMapping("/v1/cs/rooms/{roomId}")
    public ResponseEntity<ApiResponseDto<CsChatRoomResponseDto>> getCsRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.getCsRoom(roomId, userDetails.getEmail())));
    }

    // 사용자/관리자: CS 채팅방 메시지 목록 조회
    @GetMapping("/v1/cs/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponseDto<List<CsMessageResponseDto>>> getCsMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.getCsMessages(roomId, userDetails.getEmail())));
    }

    // 사용자: CS 문의 생성
    @PostMapping("/v1/cs/rooms")
    public ResponseEntity<ApiResponseDto<CsChatRoomResponseDto>> createCsRoom(
            @Valid @RequestBody CsChatRoomRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.CREATED, 
                csChatService.createCsRoom(request, userDetails.getEmail())));
    }

    // 사용자: 내 문의 목록 조회
    @GetMapping("/v1/cs/rooms")
    public ResponseEntity<ApiResponseDto<Page<CsChatRoomResponseDto>>> getMyCsRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.getMyCsRooms(page, size, userDetails.getEmail())));
    }

    // 관리자: 전체 문의 목록 조회
    @GetMapping("/v1/admin/cs/rooms")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<Page<CsChatRoomResponseDto>>> getAllCsRooms(
            @RequestParam(required = false) CsStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.getAllCsRooms(status, page, size)));
    }

    // 관리자: 문의 상태 변경
    @PatchMapping("/v1/admin/cs/rooms/{roomId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDto<CsChatRoomResponseDto>> updateCsStatus(
            @PathVariable Long roomId,
            @RequestParam CsStatus status
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.updateCsStatus(roomId, status)));
    }

    // 사용자/관리자: CS 메시지 수정
    @PatchMapping("/v1/cs/messages/{messageId}")
    public ResponseEntity<ApiResponseDto<CsMessageResponseDto>> updateCsMessage(
            @PathVariable Long messageId,
            @Valid @RequestBody SendMessageRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, 
                csChatService.updateMessage(messageId, request, userDetails.getEmail())));
    }

    // 사용자/관리자: CS 메시지 삭제
    @DeleteMapping("/v1/cs/messages/{messageId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteCsMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        csChatService.deleteMessage(messageId, userDetails.getEmail());
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponseDto.successWithNoContent());
    }
}
