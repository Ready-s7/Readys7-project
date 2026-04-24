package com.example.readys7project.domain.chat.message.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnreadCountServiceTest {

    @InjectMocks
    private UnreadCountService unreadCountService;

    @Mock
    private RedisTemplate<String, Long> unreadRedisTemplate;

    @Mock
    private ValueOperations<String, Long> valueOperations;

    @Test
    @DisplayName("unread count 증가 검증")
    void incrementTest() {
        // given
        Long roomId = 1L;
        Long userId = 1L;
        String key = "unread:1:1";
        given(unreadRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        unreadCountService.increment(roomId, userId);

        // then
        verify(valueOperations).increment(key);
    }

    @Test
    @DisplayName("unread count 초기화 검증")
    void resetTest() {
        // given
        Long roomId = 1L;
        Long userId = 1L;
        String key = "unread:1:1";
        given(unreadRedisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        unreadCountService.reset(roomId, userId);

        // then
        verify(valueOperations).set(key, 0L);
    }

    @Test
    @DisplayName("unread count 조회 검증 - 값이 있을 때")
    void getCountTest() {
        // given
        Long roomId = 1L;
        Long userId = 1L;
        String key = "unread:1:1";
        given(unreadRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn(5L);

        // when
        Long count = unreadCountService.getCount(roomId, userId);

        // then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("unread count 조회 검증 - 값이 없을 때 0 반환")
    void getCountEmptyTest() {
        // given
        Long roomId = 1L;
        Long userId = 1L;
        String key = "unread:1:1";
        given(unreadRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(key)).willReturn(null);

        // when
        Long count = unreadCountService.getCount(roomId, userId);

        // then
        assertThat(count).isEqualTo(0L);
    }
}
