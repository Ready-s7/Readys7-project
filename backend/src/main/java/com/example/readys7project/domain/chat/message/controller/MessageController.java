package com.example.readys7project.domain.chat.message.controller;

import com.example.readys7project.domain.chat.message.dto.response.MessageCursorResponseDto;
import com.example.readys7project.domain.chat.message.service.MessageService;
import com.example.readys7project.global.dto.ApiResponseDto;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // 이전 메시지 조회 (커서 기반 페이징)
    @GetMapping("/v1/chat/rooms/{roomId}/messages")
    public ResponseEntity<ApiResponseDto<MessageCursorResponseDto>> getMessages(
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        String email = userDetails.getEmail();
        return ResponseEntity.ok(ApiResponseDto.success(
                HttpStatus.OK,
                messageService.getMessages(roomId, lastMessageId, pageable, email)
        ));
    }
}
