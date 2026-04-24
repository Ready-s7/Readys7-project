package com.example.readys7project.domain.chat.message.subscriber;

import com.example.readys7project.domain.chat.cs.dto.response.CsMessageResponseDto;
import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import com.example.readys7project.global.exception.common.ErrorCode;
import com.example.readys7project.global.exception.domain.MessageException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        // RedisConfig의 설정을 타지 않는 독립적인 ObjectMapper 구성 (record 파싱 지원)
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            byte[] body = message.getBody();
            
            log.info("Redis Pub/Sub 수신 - 채널: {}", channel);

            if (channel.startsWith("chat-room:")) {
                String roomId = channel.replace("chat-room:", "");
                // 명시적으로 클래스 타입을 지정하여 역직렬화 (@class 정보 불필요)
                MessageResponseDto responseDto = objectMapper.readValue(body, MessageResponseDto.class);
                if (responseDto != null) {
                    messagingTemplate.convertAndSend("/receive/chat/rooms/" + roomId, responseDto);
                    log.info("채팅 메시지 브로드캐스트 성공: roomId={}", roomId);
                }
            } else if (channel.startsWith("cs-room:")) {
                String roomId = channel.replace("cs-room:", "");
                CsMessageResponseDto responseDto = objectMapper.readValue(body, CsMessageResponseDto.class);
                if (responseDto != null) {
                    messagingTemplate.convertAndSend("/receive/chat/cs/" + roomId, responseDto);
                    log.info("CS 메시지 브로드캐스트 성공: roomId={}", roomId);
                }
            }

        } catch (Exception e) {
            log.error("Redis 메시지 역직렬화 실패: {}", e.getMessage());
            // 예외를 밖으로 던지면 리스너 컨테이너가 중단될 수 있으므로 로그만 남깁니다.
        }
    }
}
