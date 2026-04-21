package com.example.readys7project.domain.chat.cs.controller;

import com.example.readys7project.domain.chat.cs.dto.response.CsMessageResponseDto;
import com.example.readys7project.domain.chat.cs.publisher.CsRedisMessagePublisher;
import com.example.readys7project.domain.chat.cs.service.CsChatService;
import com.example.readys7project.domain.chat.message.dto.request.SendMessageRequestDto;
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
public class CsStompController {

    private final CsChatService csChatService;
    private final CsRedisMessagePublisher csRedisMessagePublisher;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 헤더에서 토큰을 직접 추출하여 이메일 복구 (Zero-Trust)
     */
    private String getEmailFromHeader(SimpMessageHeaderAccessor headerAccessor) {
        String token = headerAccessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                jwtTokenProvider.validateToken(token);
                return jwtTokenProvider.getEmail(token);
            } catch (Exception e) {
                log.error("[CS-STOMP] 토큰 검증 실패: {}", e.getMessage());
            }
        }
        return null;
    }

    @MessageMapping("/chat/cs/{roomId}")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Payload SendMessageRequestDto request,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String email = getEmailFromHeader(headerAccessor);

        if (email == null) {
            log.error("[CS-STOMP] 메시지 전송 거부 - 인증 정보 없음 (Room: {})", roomId);
            return;
        }

        // DB 저장
        CsMessageResponseDto response = csChatService.saveMessage(roomId, request, email);

        // Redis 채널에 발행
        csRedisMessagePublisher.publish(roomId, response);
    }

    @MessageMapping("/chat/cs/{roomId}/enter")
    public void enterRoom(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String email = getEmailFromHeader(headerAccessor);

        if (email == null) {
            log.error("[CS-STOMP] 입장 처리 거부 - 인증 정보 없음 (Room: {})", roomId);
            return;
        }

        CsMessageResponseDto response = csChatService.saveSystemMessage(roomId, email, true);
        csRedisMessagePublisher.publish(roomId, response);
    }

    @MessageMapping("/chat/cs/{roomId}/leave")
    public void leaveRoom(
            @DestinationVariable Long roomId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        String email = getEmailFromHeader(headerAccessor);
        if (email == null) return;

        CsMessageResponseDto response = csChatService.saveSystemMessage(roomId, email, false);
        csRedisMessagePublisher.publish(roomId, response);
    }
}
