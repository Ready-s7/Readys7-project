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
        // 1. 먼저 레디스에서 조회
        Set<ZSetOperations.TypedTuple<String>> rankingList = searchRedisService.getTopRanking(limit);

        // 2. 레디스가 비어있다면 DB에서 조회 후 레디스에 채우기
        if (rankingList == null || rankingList.isEmpty()) {
            List<SearchRanking> dbTopRanking = searchRankingRepository.findTop10ByIsDeletedFalseOrderBySearchCountDesc();
            if (dbTopRanking.isEmpty()) {
                return Collections.emptyList();
            }
            // DB 데이터를 레디스에 복구
            for (SearchRanking ranking : dbTopRanking) {
                searchRedisService.addRankingScore(ranking.getKeyword(), ranking.getSearchCount());
            }
            // 다시 레디스에서 정렬된 상태로 조회
            rankingList = searchRedisService.getTopRanking(limit);
        }

        if (rankingList == null || rankingList.isEmpty()) {
            return Collections.emptyList();
        }

        AtomicInteger rank = new AtomicInteger(1);

        return rankingList.stream()
                .map(tuple -> {
                    String cleanKeyword = tuple.getValue();
                    return PopularRankingResponseDto.builder()
                            .ranking(rank.getAndIncrement())
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

        // 키워드가 없으면 생성, 있으면 원자적 업데이트 쿼리 실행
        if (searchRankingRepository.findByKeywordAndIsDeletedFalse(keyword).isEmpty()) {
            searchRankingRepository.save(new SearchRanking(keyword));
        } else {
            searchRankingRepository.incrementSearchCount(keyword);
        }

        // ZSet 업데이트
        searchRedisService.incrementRankingScore(keyword);
    }

}
