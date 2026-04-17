package com.example.readys7project.domain.search.service;

import com.example.readys7project.domain.search.dto.response.PopularRankingResponseDto;
import com.example.readys7project.domain.search.entity.SearchRanking;
import com.example.readys7project.domain.search.repository.SearchRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class SearchRankingService {

    private final SearchRankingRepository searchRankingRepository;
    private final SearchRedisService searchRedisService;

    // 상위 10개 인기 검색어 순위 조회
    @Transactional(readOnly = true)
    public List<PopularRankingResponseDto> getPopularRanking(int limit) {
        Set<ZSetOperations.TypedTuple<Object>> rankingList = searchRedisService.getTopRanking(limit);

        if (rankingList == null || rankingList.isEmpty()) {
            return Collections.emptyList();
        }

        AtomicInteger rank = new AtomicInteger(1);

        return rankingList.stream()
                .map(tuple -> {
                    String rawValue = String.valueOf(tuple.getValue());
                    String cleanKeyword = rawValue.replace("\"", "");
                    return PopularRankingResponseDto.builder()
                            .ranking(rank.getAndIncrement()) // 순위만 할당
                            .keyword(cleanKeyword)
                            .build();
                }).toList();
    }

    // 인기 검색어 랭킹 집계용 업데이트 로직
    @Transactional
    public void updateRankingCount(String keyword, Long userId) {

        // 5분 내 중복 검색이면 바로 스킵
        if (!searchRedisService.isFirstSearch(userId, keyword)) {
            return;
        }

        // DB 업데이트
        SearchRanking searchRanking = searchRankingRepository
                .findByKeywordAndIsDeletedFalse(keyword)
                .orElseGet(() -> searchRankingRepository.save(new SearchRanking((keyword))));

        searchRanking.increaseSearchCount();

        // ZSet 업데이트
        searchRedisService.incrementRankingScore(keyword);
    }

}

/* Set<ZSetOperations.TypedTuple<String>> -> Redis에서 꺼내온 [ 검색어 + 점수 ] 세트들의 묶음
        Object -> 실제 데이터, TypedTuple -> Redis ZSet의 핵심인 [ 값(Value) + 점수(Score)]를 한번에 담고 있는 상자
        getValue()를 호출하면 -> "Java"가 나오고, getScore() -> "100.0"이 나옴
        Set<> -> ZSet은 중복을 허용하지 않기 때문에 Set 인터페이스로 사용
        reverseRangeWithScores -> 순위만 가져오지 말고, 점수까지고 같이 달라는 의미*/

        /* AtomicInteger -> 자바에서 멀티스레드 환경에서도 안전하게 숫자를 계산할 수 있게해주는 원자적 변수,
         여러 사람이 동시에 접근해도 숫자가 꼬이지 않도록 특수 설계된 전용 계산기
         AtomicInteger rank = new AtomicInteger(1);
         -> 정수값을 가진 변수를 만드는데, 초기값을 1로 설정하겠다는 의미
         AtomicInteger를 사용하는 이유 : 자바의 람다 안에서는 지역변수를 수정할 수 없는 규칙이 있다,
         하지만 AtomicInteger를는 객체이기 때문에, 람다 안에서도 내부 값을 안전하게 바꿀 수 있다*/

// getAndIncrement() -> "현재 값을 먼저 가져오고, 그 다음에 1을 증가시켜라는 의미
