package com.example.readys7project.global.lock.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class LockRedisRepository {

    private final RedisTemplate<String, String> redisTemplate;

    // Lua Script: get과 del을 원자적으로 실행 (본인 UUID 검증 후 삭제)
    private static final String UNLOCK_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "return redis.call('del', KEYS[1]) " +
            "else return 0 end";

    // Lock 획득: SETNX + TTL
    public boolean lock(String key, String value, long timeout, TimeUnit timeUnit) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue()
                        .setIfAbsent(key, value, timeout, timeUnit)
        );
    }

    public boolean unlock(String key, String value) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(UNLOCK_SCRIPT, Long.class);
        Long result = redisTemplate.execute(
                script,
                List.of(key),
                value
        );
        return Long.valueOf(1L).equals(result);
    }
}
