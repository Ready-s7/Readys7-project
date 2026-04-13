package com.example.readys7project.domain.chat.message.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnreadCountService {

    private final RedisTemplate<String, Long> unreadRedisTemplate;

    // unread count 증가 (메시지 수신 시)
    public void increment(Long roomId, Long userId) {
        String key = generateKey(roomId, userId);
        unreadRedisTemplate.opsForValue().increment(key);
    }

    // unread count 초기화 (채팅방 입장 시)
    public void reset(Long roomId, Long userId) {
        String key = generateKey(roomId, userId);
        unreadRedisTemplate.opsForValue().set(key, 0L);
    }

    // unread count 조회
    public Long getCount(Long roomId, Long userId) {
        String key = generateKey(roomId, userId);
        Long count = unreadRedisTemplate.opsForValue().get(key);
        return count == null ? 0L : count;
    }

    private String generateKey(Long roomId, Long userId) {
        return "unread:" + roomId + ":" + userId;
    }
}
