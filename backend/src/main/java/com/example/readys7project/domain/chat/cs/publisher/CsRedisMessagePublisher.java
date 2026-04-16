package com.example.readys7project.domain.chat.cs.publisher;

import com.example.readys7project.domain.chat.cs.dto.CsMessageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CsRedisMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(Long roomId, CsMessageResponseDto message) {
        // 채널명: cs-room:{roomId}
        redisTemplate.convertAndSend("cs-room:" + roomId, message);
    }
}
