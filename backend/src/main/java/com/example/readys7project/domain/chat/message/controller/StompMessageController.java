package com.example.readys7project.domain.chat.message.controller;

import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.domain.chat.message.publisher.RedisMessagePublisher;
import com.example.readys7project.domain.chat.message.service.MessageService;
import com.example.readys7project.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class StompMessageController {

    private final MessageService messageService;
    private final RedisMessagePublisher redisMessagePublisher;
    private final JwtTokenProvider jwtTokenProvider;

    private String getEmail(SimpMessageHeaderAccessor accessor) {
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                String pureToken = token.substring(7);
                jwtTokenProvider.validateToken(pureToken);
                return jwtTokenProvider.getEmail(pureToken);
            } catch (Exception e) {
                log.error("Token invalid: {}", e.getMessage());
            }
        }
        return null;
    }

    @MessageMapping("/chat/rooms/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload SendMessageRequestDto request, SimpMessageHeaderAccessor accessor) {
        String email = getEmail(accessor);
        if (email == null) return;
        MessageResponseDto response = messageService.saveMessage(roomId, request, email);
        redisMessagePublisher.publish(roomId, response);
    }

    @MessageMapping("/chat/rooms/{roomId}/enter")
    public void enterRoom(@DestinationVariable Long roomId, SimpMessageHeaderAccessor accessor) {
        String email = getEmail(accessor);
        if (email == null) return;
        MessageResponseDto response = messageService.saveSystemMessage(roomId, email, true);
        redisMessagePublisher.publish(roomId, response);
    }

    @MessageMapping("/chat/rooms/{roomId}/leave")
    public void leaveRoom(@DestinationVariable Long roomId, SimpMessageHeaderAccessor accessor) {
        String email = getEmail(accessor);
        if (email == null) return;
        MessageResponseDto response = messageService.saveSystemMessage(roomId, email, false);
        redisMessagePublisher.publish(roomId, response);
    }
}
