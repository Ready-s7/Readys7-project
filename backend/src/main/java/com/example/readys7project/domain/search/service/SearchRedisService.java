package com.example.readys7project.domain.search.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SearchRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

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

    public Set<ZSetOperations.TypedTuple<Object>> getTopRanking(int limit) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(RANKING_KEY, 0, limit - 1);
    }


}

/* Redis는 데이터를 어떻게 전달해줄까?
 Redis의 ZSet은 내부적으로 [값(Value), 점수(Score)]가 묶인 상태로 저장되지만, 우리가 자바에서 꺼낼 때는
 어떤 메서드를 쓰느냐에 따라 데이터의 형태가 달라짐
 1. reverseRange를 썼을 때 -> 점수 빼고 이름만 보냄 Set<String>을 줌
 -> 결과 : ["Java", "Spring", "Redis"]
 2. reverseRangeWithScores를 썻을 때 -> 이름이랑 점수를 세트로 보내줌
 -> 결과 : [(Java, 100.0), (Spring, 110.0), (Redis, 150.0)]
 reverseRangeWithScores을 사용하면 좋은점이 단순히 이름만 가져오는게 아닌, 몇 번 검색이 됐는지도 확인할 수 있음*/

/* 검색의 전체적인 흐름

 1. 검색 발생 (저장)
 사용자가 검색창에 Java를 입력하고 엔터를 치면 getTotalSearchV1이 실행됨

 2. 점수 업데이트 (Write)
 updateRankingCount 로직이 돌아가며 두 군데에 신호를 보냄,
 MySQL -> Java 검색 횟수 1 증가시켜 (영구 보존용)
 Redis (ZSet) -> Java가 이미 있으면 +1, 없으면 새로 만들고 점수 1 부여해줘 -> 이때 Redis는 실시간으로 순위를 재계산함

 3. 순위 요청 (Read)
 사용자가 메인 페이지에서 접속하거나 인기 검색어 버튼을 누르면 getPopularRanking이 호출됨

 4. 데이터 가공
 Redis에서 상위 10개를 Set<TypedTuple<String>> 형태로 받아옴,
 4.1 순수 키워드 추출 -> getValue()로 "Java"만 쏙 뽑아옴
 4.2 따옴표 청소 -> JSON 직렬화로 생긴 ""를 replace로 제거
 4.3 순위 매기기 -> AtomicInteger 가 1,2,3... 번호를 붙여줌

 5. 최종 응답
 PopularRankingResponseDto 리스트가 JSON 형태로 사용자 화면에 뿌려짐*/
