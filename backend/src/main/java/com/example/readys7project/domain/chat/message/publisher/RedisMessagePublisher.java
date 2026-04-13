package com.example.readys7project.domain.chat.message.publisher;

import com.example.readys7project.domain.chat.message.dto.response.MessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long roomId, MessageResponseDto message) {
        // 채널명: chat-room:{roomId}
        redisTemplate.convertAndSend("chat-room:" + roomId, message);
    }
}
