package com.example.readys7project.domain.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SearchRedisService {

    private final StringRedisTemplate redisTemplate;

    // 랭킹 키
    private static final String RANKING_KEY = "ranking";
    // DEDUP -> Deduplication 중복 제거의 줄임말
    private static final String DEDUP_PREFIX = "dedup:";
    // 검색 후 5분 동안은 동일 키워드 검색 카운팅 X, 5분 이후로 카운팅 O
    private static final long DEDUP_TTL_SECONDS = 300;

    // 5분 내 동일 유저 + 동일 키워드 첫 검색 여부 확인
    public boolean isFirstSearch(Long userId, String keyword) {
        String dedupKey = DEDUP_PREFIX + userId + ":" + keyword;
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", DEDUP_TTL_SECONDS, TimeUnit.SECONDS);

        return Boolean.TRUE.equals(result);
    }

    // ZSet 점수 +1
    public void incrementRankingScore(String keyword) {
        redisTemplate.opsForZSet().incrementScore(RANKING_KEY, keyword, 1);
    }

    // ZSet 점수 직접 설정 (Overwrite)
    public void addRankingScore(String keyword, double score) {
        redisTemplate.opsForZSet().add(RANKING_KEY, keyword, score);
    }

    public Set<ZSetOperations.TypedTuple<String>> getTopRanking(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANKING_KEY, 0, limit - 1);
    }


}
