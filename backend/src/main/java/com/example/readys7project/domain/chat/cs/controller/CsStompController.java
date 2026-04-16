package com.example.readys7project.domain.chat.cs.controller;

import com.example.readys7project.domain.chat.cs.dto.CsMessageResponseDto;
import com.example.readys7project.domain.chat.cs.publisher.CsRedisMessagePublisher;
import com.example.readys7project.domain.chat.cs.service.CsChatService;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class CsStompController {

    private final CsChatService csChatService;
    private final CsRedisMessagePublisher csRedisMessagePublisher;

    // 클라이언트 SEND 경로: /send/chat/cs/{roomId}
    // 클라이언트 SUBSCRIBE 경로: /receive/chat/cs/{roomId}
    @MessageMapping("/chat/cs/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequestDto request,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();

        // DB 저장
        CsMessageResponseDto response = csChatService.saveMessage(roomId, request, email);

        // Redis 채널에 발행 → Subscriber가 받아서 STOMP 브로드캐스트
        csRedisMessagePublisher.publish(roomId, response);
    }

    // 입장
    @MessageMapping("/chat/cs/{roomId}/enter")
    public void enterRoom(
            @DestinationVariable Long roomId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();
        CsMessageResponseDto response = csChatService.saveSystemMessage(roomId, email, true);
        csRedisMessagePublisher.publish(roomId, response);
    }

    // 퇴장
    @MessageMapping("/chat/cs/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            Authentication authentication
    ) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String email = userDetails.getEmail();
        CsMessageResponseDto response = csChatService.saveSystemMessage(roomId, email, false);
        csRedisMessagePublisher.publish(roomId, response);
    }
}
