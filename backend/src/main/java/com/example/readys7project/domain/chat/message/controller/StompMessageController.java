package com.example.readys7project.domain.chat.message.controller;

import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.domain.chat.message.publisher.RedisMessagePublisher;
import com.example.readys7project.domain.chat.message.service.MessageService;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class StompMessageController {

    private final MessageService messageService;
    private final RedisMessagePublisher redisMessagePublisher;

    // 클라이언트 SEND 경로: /send/chat/rooms/{roomId}
    // 클라이언트 SUBSCRIBE 경로: /receive/chat/rooms/{roomId}
    @MessageMapping("/chat/rooms/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequestDto request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();

        // DB 저장
        MessageResponseDto response = messageService.saveMessage(roomId, request, email);

        // Redis 채널에 발행 → Subscriber가 받아서 STOMP 브로드캐스트
        redisMessagePublisher.publish(roomId, response);
    }

    // 입장
    @MessageMapping("/chat/rooms/{roomId}/enter")
    public void enterRoom(
            @DestinationVariable Long roomId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();
        MessageResponseDto response = messageService.saveSystemMessage(roomId, email, true);
        redisMessagePublisher.publish(roomId, response);
    }

    // 퇴장
    @MessageMapping("/chat/rooms/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();
        MessageResponseDto response = messageService.saveSystemMessage(roomId, email, false);
        redisMessagePublisher.publish(roomId, response);
    }
}
