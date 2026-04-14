package com.example.readys7project.domain.chat.chatroom.controller;

import com.example.readys7project.domain.chat.chatroom.dto.ChatRoomDto;
import com.example.readys7project.domain.chat.chatroom.dto.request.CreateChatRoomRequestDto;
import com.example.readys7project.domain.chat.chatroom.service.ChatRoomService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 생성
    @PostMapping("/v1/chat/rooms")
    public ResponseEntity<ApiResponseDto<ChatRoomDto>> createChatRoom(
            @Valid @RequestBody CreateChatRoomRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String email = userDetails.getEmail();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.success(HttpStatus.CREATED, chatRoomService.createChatRoom(request, email)));
    }

    // 내 채팅방 목록 조회
    @GetMapping("/v1/chat/rooms")
    public ResponseEntity<ApiResponseDto<Page<ChatRoomDto>>> getMyChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(HttpStatus.OK, chatRoomService.getMyChatRooms(email, pageable)));
    }
}
